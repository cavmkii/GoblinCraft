package com.cavmkii.seancraft.carry;

import com.cavmkii.seancraft.SeanCraft;
import com.cavmkii.seancraft.tetris.ContainerGrid;
import com.cavmkii.seancraft.tetris.Tetris;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Picking up a chest or barrel, contents and all: Shift + left-click to pick it up, Shift + right-click to set it
 * down. While you carry it you walk slowly, can't sprint, can't switch hotbar slots, use items, fight, mine or open
 * anything. If you die carrying it, it and everything in it drops where you fell.
 */
public final class Carry {
    /** What's being carried: the block and its saved block entity. */
    public record Carried(BlockState state, CompoundTag data) {
        static final Codec<Carried> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Carried::state),
                CompoundTag.CODEC.fieldOf("data").forGetter(Carried::data)
        ).apply(i, Carried::new));
    }

    public static final AttachmentType<Carried> CARRIED = AttachmentRegistry.<Carried>builder()
            .persistent(Carried.CODEC)
            .buildAndRegister(SeanCraft.id("carried"));

    /** Server to clients: an entity started (state present) or stopped carrying a block. */
    public record State(int entity, Optional<BlockState> block) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(SeanCraft.id("carry_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::entity,
                ByteBufCodecs.optional(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)), State::block,
                State::new);
        @Override public Type<State> type() { return TYPE; }
    }

    private static final String FILTER_KEY = "seancraft:filter";
    private static final net.minecraft.resources.ResourceLocation SLOW = SeanCraft.id("carrying");

    private Carry() {}

    public static boolean carrying(Player player) {
        return player.hasAttached(CARRIED);
    }

    private static boolean carriable(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock;
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(State.TYPE, State.CODEC);

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (carrying(player)) {
                return InteractionResult.FAIL;
            }
            if (!player.isShiftKeyDown() || !Tetris.active(player) || !carriable(level.getBlockState(pos))) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer sp) {
                pickUp(sp, pos);
            }
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!carrying(player)) {
                return InteractionResult.PASS;
            }
            if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
                setDown(sp, hit);
            }
            // Nothing else while your hands are full, and no second try with the off hand.
            return player.level().isClientSide && player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND
                    ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        });
        UseItemCallback.EVENT.register((player, level, hand) ->
                carrying(player) ? InteractionResultHolder.fail(player.getItemInHand(hand)) : InteractionResultHolder.pass(player.getItemInHand(hand)));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> carrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> carrying(player) ? InteractionResult.FAIL : InteractionResult.PASS);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && carrying(player)) {
                dropAll(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> broadcast(handler.player));
        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> {
            if (tracked instanceof ServerPlayer other && carrying(other)) {
                ServerPlayNetworking.send(player, new State(other.getId(), Optional.of(other.getAttached(CARRIED).state())));
            }
        });
    }

    /** Hotbar slot each carrier is locked to. */
    private static final java.util.Map<java.util.UUID, Integer> LOCKED = new java.util.HashMap<>();

    private static void tick(ServerPlayer player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (!carrying(player)) {
            if (speed != null) {
                speed.removeModifier(SLOW);
            }
            LOCKED.remove(player.getUUID());
            return;
        }
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
        // Hands full: no switching hotbar slots, and no menus.
        int locked = LOCKED.computeIfAbsent(player.getUUID(), id -> player.getInventory().selected);
        if (player.getInventory().selected != locked) {
            player.getInventory().selected = locked;
            player.connection.send(new ClientboundSetCarriedItemPacket(locked));
        }
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
    }

    private static void pickUp(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (!carriable(state) || be == null || !player.mayInteract(level, pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36) {
            return;
        }
        if (com.cavmkii.seancraft.tarkov.SpawnPoints.safe(level, Vec3.atCenterOf(pos))) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Spawn point supplies stay put."), true);
            return;
        }
        CompoundTag data = be.saveWithoutMetadata(level.registryAccess());
        Integer filter = be.getAttached(ContainerGrid.FILTER);
        if (filter != null) {
            data.putInt(FILTER_KEY, filter);
        }
        if (state.getBlock() instanceof ChestBlock) {
            // A double chest comes apart: you take this half, the other half stays as a single chest.
            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        if (be instanceof Container c) {
            c.clearContent(); // so removing the block doesn't spill it
        }
        level.removeBlock(pos, false);
        player.setAttached(CARRIED, new Carried(state, data));
        LOCKED.put(player.getUUID(), player.getInventory().selected);
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 0.8f);
        broadcast(player);
    }

    private static void setDown(ServerPlayer player, BlockHitResult hit) {
        Carried carried = player.getAttached(CARRIED);
        ServerLevel level = player.serverLevel();
        if (carried == null) {
            return;
        }
        Block block = carried.state.getBlock();
        BlockPlaceContext ctx = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
        BlockPos pos = ctx.getClickedPos();
        if (!ctx.canPlace() || !player.mayInteract(level, pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36) {
            return;
        }
        // Face it the way a placed one would (you're sneaking, so a chest won't join up with its neighbours).
        BlockState state = block.getStateForPlacement(ctx);
        if (state == null || !state.canSurvive(level, pos)) {
            return;
        }
        if (state.getBlock() instanceof ChestBlock) {
            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.loadWithComponents(carried.data, level.registryAccess());
            if (carried.data.contains(FILTER_KEY)) {
                be.setAttached(ContainerGrid.FILTER, carried.data.getInt(FILTER_KEY));
            }
            be.setChanged();
        }
        player.removeAttached(CARRIED);
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.8f);
        broadcast(player);
    }

    /** Death: the chest and what's in it end up on the ground. */
    private static void dropAll(ServerPlayer player) {
        Carried carried = player.getAttached(CARRIED);
        player.removeAttached(CARRIED);
        Level level = player.level();
        NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(carried.data, items, level.registryAccess());
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                Block.popResource(level, player.blockPosition(), stack);
            }
        }
        Block.popResource(level, player.blockPosition(), new ItemStack(carried.state.getBlock()));
        broadcast(player);
    }

    private static void broadcast(ServerPlayer player) {
        Carried carried = player.getAttached(CARRIED);
        State state = new State(player.getId(), Optional.ofNullable(carried).map(Carried::state));
        ServerPlayNetworking.send(player, state);
        for (ServerPlayer other : PlayerLookup.tracking(player)) {
            if (other != player) {
                ServerPlayNetworking.send(other, state);
            }
        }
    }
}
