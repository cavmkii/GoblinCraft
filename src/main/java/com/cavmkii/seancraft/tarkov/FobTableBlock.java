package com.cavmkii.seancraft.tarkov;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A FOB table (2.6): while you're in its chunk, the raid timer stops, out of a shared per-raid budget. No stash, no
 * modules, no respawn, and not a safe zone.
 */
public class FobTableBlock extends Block {
    public FobTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            Fobs.get(server).add(new ChunkPos(pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this)) {
            Fobs.get(server).remove(new ChunkPos(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("While you're in this chunk, the raid timer stops (out of your FOB time), "
                    + "unless monsters are near or you were just hit.").withStyle(ChatFormatting.GRAY), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The item: one table per chunk, and none inside a hideout or a spawn point's safe zone, where it would do nothing. */
    public static class Item extends BlockItem {
        public Item(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        protected boolean canPlace(BlockPlaceContext context, BlockState state) {
            if (context.getLevel() instanceof ServerLevel level) {
                BlockPos pos = context.getClickedPos();
                String problem = null;
                if (Fobs.get(level).has(new ChunkPos(pos))) {
                    problem = "This chunk already has a FOB table.";
                } else if (Hideouts.at(level, Vec3.atCenterOf(pos)) != null || SpawnPoints.safe(level, Vec3.atCenterOf(pos))) {
                    problem = "A FOB table does nothing in a safe zone.";
                }
                if (problem != null) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(Component.literal(problem).withStyle(ChatFormatting.RED), true);
                        // The client already took the item out of the stack when it guessed the placement would work.
                        context.getPlayer().containerMenu.sendAllDataToRemote();
                    }
                    return false;
                }
            }
            return super.canPlace(context, state);
        }
    }
}
