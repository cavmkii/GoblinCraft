package com.cavmkii.seancraft.tetris;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.SeanCraft;
import com.cavmkii.seancraft.tarkov.Hideouts;
import com.cavmkii.seancraft.tarkov.Stash;
import com.cavmkii.seancraft.tarkov.Tarkov;
import com.cavmkii.seancraft.tarkov.TarkovData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

/**
 * The Tetris inventory, server side. The vanilla hotbar, armor and off-hand stay as they are; the 27 main slots
 * are an inbox that is emptied into the grids every tick, and pickups go straight to the grids. Chests and barrels
 * open on the GEAR tab instead of their own screen. Creative players keep the vanilla inventory.
 */
public final class Tetris {
    public static final AttachmentType<InvData> DATA = AttachmentRegistry.<InvData>builder()
            .persistent(InvData.CODEC)
            .copyOnDeath()
            .initializer(InvData::new)
            .buildAndRegister(SeanCraft.id("inventory"));

    public static final Map<GearSpec, Item> GEAR = new EnumMap<>(GearSpec.class);

    static {
        for (GearSpec spec : GearSpec.values()) {
            GEAR.put(spec, Registry.register(BuiltInRegistries.ITEM, SeanCraft.id(spec.id), new GearItem(spec, new Item.Properties().stacksTo(1))));
        }
    }

    /** What the client last heard, for client-side code that reads the grids (bows looking for arrows). */
    public static InvData clientView = new InvData();

    private static final double CONTAINER_REACH_SQR = 64;
    private static final int TABLE_RANGE = 6;

    static final class Runtime {
        boolean open;
        BlockPos container;
        boolean dirty = true;
        int hash;
    }

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    private Tetris() {}

    public static InvData data(Player player) {
        return player.level().isClientSide ? clientView : player.getAttachedOrCreate(DATA);
    }

    private static Runtime rt(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), id -> new Runtime());
    }

    public static void markDirty(Player player) {
        if (player instanceof ServerPlayer sp) {
            rt(sp).dirty = true;
        }
    }

    /** The Tetris inventory is in charge for survival and adventure players. */
    public static boolean active(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    public static void init() {
        TetrisNet.register();
        ContainerGrid.init();
        ServerTickEvents.END_SERVER_TICK.register(Tetris::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> rt(handler.player).dirty = true);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RUNTIME.remove(handler.player.getUUID()));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Runtime rt = rt(newPlayer);
            rt.dirty = true;
            rt.container = null;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!active(player) || player.isSecondaryUseActive()) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof BarrelBlock)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer sp) {
                openContainer(sp, hit.getBlockPos());
            }
            return InteractionResult.SUCCESS;
        });

        ServerPlayNetworking.registerGlobalReceiver(TetrisNet.Move.TYPE, (p, ctx) -> {
            move(ctx.player(), p.from(), p.to(), p.rotated(), p.count());
            sync(ctx.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(TetrisNet.QuickMove.TYPE, (p, ctx) -> {
            quickMove(ctx.player(), p.from());
            sync(ctx.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(TetrisNet.Action.TYPE, (p, ctx) -> {
            action(ctx.player(), p.action());
            sync(ctx.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(TetrisNet.PanelClick.TYPE, (p, ctx) -> {
            panelClick(ctx.player(), p.loc(), p.rotated(), p.button());
            sync(ctx.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(TetrisNet.Craft.TYPE, (p, ctx) -> {
            if (!p.id().isEmpty()) {
                craft(ctx.player(), p.id(), p.times());
                sync(ctx.player());
            }
            ServerPlayNetworking.send(ctx.player(), new TetrisNet.CraftList(craftable(ctx.player())));
        });
    }

    // ================================================================== tick

    private static void tick(MinecraftServer server) {
        boolean periodic = server.getTickCount() % 10 == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || !active(player)) {
                continue;
            }
            Runtime rt = rt(player);
            InvData data = data(player);
            TarkovData tarkov = Tarkov.data(player);
            if (!tarkov.starterGiven) {
                tarkov.starterGiven = true;
                if (data.rig.isEmpty()) {
                    data.rig = new ItemStack(GEAR.get(GearSpec.RIG_SCAV));
                }
                if (data.backpack.isEmpty()) {
                    data.backpack = new ItemStack(GEAR.get(GearSpec.PACK_SLING));
                }
                player.sendSystemMessage(Component.literal("You have a Scav vest, a Sling bag and an Alpha pouch. Press E to see them.")
                        .withStyle(ChatFormatting.GRAY));
                rt.dirty = true;
            }
            if (!data.migrated) {
                data.migrated = true;
                Grid pouch = pouchGrid(player, data);
                for (int i = 0; i < tarkov.secure.size(); i++) {
                    ItemStack s = tarkov.secure.get(i);
                    if (!s.isEmpty()) {
                        if (!pouch.add(s)) {
                            data.unsorted.add(s);
                        }
                        tarkov.secure.set(i, ItemStack.EMPTY);
                    }
                }
                rt.dirty = true;
            }
            // Empty the vanilla main slots into the grids.
            Inventory inv = player.getInventory();
            for (int i = Inventory.getSelectionSize(); i < inv.items.size(); i++) {
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty()) {
                    inv.items.set(i, ItemStack.EMPTY);
                    storeInGrids(player, data, s);
                    if (!s.isEmpty()) {
                        data.unsorted.add(s);
                    }
                    rt.dirty = true;
                }
            }
            if (purge(data)) {
                rt.dirty = true;
            }
            if (server.getTickCount() % 20 == 0 && !data.unsorted.isEmpty()) {
                for (int i = data.unsorted.size() - 1; i >= 0; i--) {
                    ItemStack s = data.unsorted.get(i);
                    storeInGrids(player, data, s);
                    if (s.isEmpty()) {
                        data.unsorted.remove(i);
                        rt.dirty = true;
                    }
                }
                if (!rt.open && server.getTickCount() % 600 == 0 && !data.unsorted.isEmpty()) {
                    player.displayClientMessage(Component.literal(data.unsorted.size() + " unsorted item(s): open your inventory to make room.")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
            }
            if (rt.container != null && containerAt(player, rt.container) == null) {
                rt.container = null;
                rt.dirty = true;
            }
            // Other systems use up grid items in place (arrows, meds, hideout costs); notice it here.
            if (periodic) {
                int hash = hash(data);
                if (hash != rt.hash) {
                    rt.hash = hash;
                    rt.dirty = true;
                }
            }
            if (rt.dirty || (rt.open && periodic)) {
                sync(player);
            }
        }
    }

    private static int hash(InvData data) {
        int h = 1;
        for (GridId id : GridId.values()) {
            for (Placed p : data.list(id)) {
                h = h * 31 + p.stack.getCount() * 7 + p.x * 3 + p.y + System.identityHashCode(p.stack.getItem());
            }
        }
        return h;
    }

    /** Drops emptied stacks from the grids. True if anything changed. */
    private static boolean purge(InvData data) {
        boolean changed = false;
        for (GridId id : GridId.values()) {
            changed |= data.list(id).removeIf(p -> p.stack.isEmpty());
        }
        changed |= data.unsorted.removeIf(ItemStack::isEmpty);
        return changed;
    }

    static void sync(ServerPlayer player) {
        Runtime rt = rt(player);
        rt.dirty = false;
        InvData d = data(player);
        TarkovData tarkov = Tarkov.data(player);
        List<TetrisNet.ContainerView> views = new ArrayList<>();
        Hideouts.Hideout home = Hideouts.at(player.serverLevel(), player.position());
        if (rt.open || rt.container != null) {
            for (Shown c : containers(player)) {
                ContainerGrid g = c.grid();
                List<ItemStack> overflow = new ArrayList<>();
                for (int slot : g.overflow) {
                    overflow.add(c.container.getItem(slot).copy());
                }
                views.add(new TetrisNet.ContainerView(c.pos, c.name, g.width, g.height, g.view(), overflow, List.copyOf(g.overflow)));
            }
        }
        // Copies: on a single-player server the payload object reaches the client as is.
        ServerPlayNetworking.send(player, new TetrisNet.Sync(d.rig.copy(), d.backpack.copy(), copy(d.pockets), copy(d.rigItems),
                copy(d.backpackItems), copy(d.pouch), tarkov.tier.width, tarkov.tier.height,
                d.unsorted.stream().map(ItemStack::copy).toList(), home != null, views));
    }

    private static List<Placed> copy(List<Placed> list) {
        List<Placed> out = new ArrayList<>();
        for (Placed p : list) {
            out.add(new Placed(p.stack.copy(), p.x, p.y, p.rotated));
        }
        return out;
    }

    private static Grid pouchGrid(Player player, InvData data) {
        TarkovData tarkov = Tarkov.data(player);
        return data.grid(GridId.POUCH, tarkov.tier.width, tarkov.tier.height);
    }

    private static Grid grid(Player player, InvData data, GridId id) {
        return id == GridId.POUCH ? pouchGrid(player, data) : data.grid(id, 0, 0);
    }

    // ================================================================== storing

    /** Grids that pickups fill, in order. The pouch is only filled by hand. */
    private static final GridId[] AUTO = {GridId.POCKETS, GridId.RIG, GridId.BACKPACK};

    /** Tops up and places a stack in pockets, rig and backpack. Leaves what didn't fit in the stack. */
    private static void storeInGrids(Player player, InvData data, ItemStack stack) {
        for (GridId id : AUTO) {
            Grid g = grid(player, data, id);
            if (g != null) {
                g.merge(stack);
            }
        }
        for (GridId id : AUTO) {
            if (stack.isEmpty()) {
                return;
            }
            Grid g = grid(player, data, id);
            if (g != null && g.add(stack.copy())) {
                stack.setCount(0);
            }
        }
    }

    /** Weapons and tools go to an empty hotbar slot before the grids. */
    private static boolean handItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof net.minecraft.world.item.DiggerItem || item instanceof AxeItem
                || item instanceof MaceItem || item instanceof TridentItem || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof ShieldItem || item instanceof FishingRodItem
                || com.cavmkii.seancraft.compat.GrappleCompat.isGrappleItem(item);
    }

    /**
     * Pickups and gifts (anything calling {@code Inventory.add}): top up the hotbar and the grids, then hotbar for
     * weapons and tools, then free grid space, then any free hotbar slot. What doesn't fit stays in the stack.
     * Returns what vanilla would: whether anything was taken.
     */
    public static boolean pickup(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int before = stack.getCount();
        Inventory inv = player.getInventory();
        InvData data = data(player);
        int hotbar = Inventory.getSelectionSize();
        for (int i = 0; i < hotbar && !stack.isEmpty(); i++) {
            ItemStack s = inv.items.get(i);
            if (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int n = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(n);
                stack.shrink(n);
            }
        }
        if (!stack.isEmpty() && handItem(stack)) {
            freeHotbar(inv, stack);
        }
        if (!stack.isEmpty()) {
            storeInGrids(player, data, stack);
        }
        if (!stack.isEmpty()) {
            freeHotbar(inv, stack);
        }
        if (stack.getCount() != before) {
            rt(player).dirty = true;
        }
        return stack.isEmpty() || stack.getCount() < before;
    }

    private static void freeHotbar(Inventory inv, ItemStack stack) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (inv.items.get(i).isEmpty()) {
                inv.items.set(i, stack.copyAndClear());
                inv.items.get(i).setPopTime(5);
                return;
            }
        }
    }

    // ================================================================== carried items, for other systems

    /**
     * Live stacks the player carries in a fixed order: vanilla main inventory (36), off-hand, then pockets, rig,
     * backpack and pouch. Shrinking one uses it up; emptied grid entries are cleared next tick and the client is told
     * within half a second. Armor is not included.
     */
    public static List<ItemStack> carried(Player player) {
        List<ItemStack> out = new ArrayList<>(player.getInventory().items);
        out.add(player.getOffhandItem());
        InvData data = data(player);
        for (GridId id : GridId.values()) {
            for (Placed p : data.list(id)) {
                out.add(p.stack);
            }
        }
        return out;
    }

    /** A projectile from rig, pockets or backpack, when hands and hotbar have none. */
    public static ItemStack gridProjectile(Player player, Predicate<ItemStack> supported) {
        if (!active(player)) {
            return ItemStack.EMPTY;
        }
        InvData data = data(player);
        for (GridId id : new GridId[] {GridId.RIG, GridId.POCKETS, GridId.BACKPACK}) {
            for (Placed p : data.list(id)) {
                if (!p.stack.isEmpty() && supported.test(p.stack)) {
                    return p.stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // ================================================================== containers

    /** A container on the right of GEAR. {@code holder} keeps its grid layout (for a double chest, one fixed half). */
    record Shown(BlockPos pos, String name, Container container, BlockEntity holder) {
        ContainerGrid grid() {
            return ContainerGrid.of(container, holder);
        }
    }

    /** The opened container and, in a hideout, every stash chest. Same order as the client sees. */
    static List<Shown> containers(ServerPlayer player) {
        Runtime rt = rt(player);
        List<Shown> out = new ArrayList<>();
        Set<BlockPos> covered = new HashSet<>();
        ServerLevel level = player.serverLevel();
        if (rt.container != null) {
            Container c = containerAt(player, rt.container);
            if (c != null) {
                BlockState state = level.getBlockState(rt.container);
                covered.add(rt.container);
                BlockPos holderPos = rt.container;
                if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    BlockPos other = rt.container.relative(ChestBlock.getConnectedDirection(state));
                    covered.add(other);
                    if (other.asLong() < holderPos.asLong()) {
                        holderPos = other;
                    }
                }
                String name = c instanceof CompoundContainer ? "Large Chest"
                        : level.getBlockEntity(rt.container) instanceof BaseContainerBlockEntity be ? be.getDisplayName().getString() : "Container";
                BlockEntity holder = level.getBlockEntity(holderPos);
                if (holder != null) {
                    out.add(new Shown(rt.container, name, c, holder));
                }
            }
        }
        Hideouts.Hideout home = Hideouts.at(level, player.position());
        if (home != null) {
            for (Container c : Stash.containers(level, home)) {
                if (c instanceof BlockEntity be && !covered.contains(be.getBlockPos())) {
                    String name = be instanceof BaseContainerBlockEntity base ? base.getDisplayName().getString() : "Chest";
                    out.add(new Shown(be.getBlockPos(), "Stash: " + name, c, be));
                }
            }
        }
        return out;
    }

    private static Container containerAt(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > CONTAINER_REACH_SQR) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, false);
        }
        if (state.getBlock() instanceof BarrelBlock && level.getBlockEntity(pos) instanceof Container c) {
            return c;
        }
        return null;
    }

    private static void openContainer(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity be && !be.canOpen(player)) {
            return;
        }
        Container c = containerAt(player, pos);
        if (c == null) {
            return;
        }
        Runtime rt = rt(player);
        rt.container = pos;
        rt.open = true;
        BlockState state = level.getBlockState(pos);
        level.playSound(null, pos, state.getBlock() instanceof BarrelBlock ? SoundEvents.BARREL_OPEN : SoundEvents.CHEST_OPEN,
                SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
        player.awardStat(state.getBlock() instanceof BarrelBlock ? net.minecraft.stats.Stats.OPEN_BARREL : net.minecraft.stats.Stats.OPEN_CHEST);
        sync(player);
        ServerPlayNetworking.send(player, new TetrisNet.Open(0));
    }

    /** Puts a stack into a container's grid: top up first, then wherever it fits. Leaves the rest in the stack. */
    private static void storeIn(Shown c, ItemStack stack) {
        ContainerGrid g = c.grid();
        g.insertAuto(stack);
        g.save();
        c.container.setChanged();
    }

    // ================================================================== moving

    /** A place an item can be taken from. */
    private interface Ref {
        ItemStack get();

        void set(ItemStack stack);
    }

    private static Ref ref(ServerPlayer player, InvData data, Loc loc) {
        Inventory inv = player.getInventory();
        switch (loc.kind()) {
            case Loc.HAND -> {
                if (loc.a() < 0 || loc.a() >= Inventory.getSelectionSize()) {
                    return null;
                }
                return new Ref() {
                    public ItemStack get() { return inv.items.get(loc.a()); }
                    public void set(ItemStack s) { inv.items.set(loc.a(), s); }
                };
            }
            case Loc.EQUIP -> {
                return switch (loc.a()) {
                    case 0, 1, 2, 3 -> new Ref() {
                        public ItemStack get() { return inv.armor.get(3 - loc.a()); }
                        public void set(ItemStack s) { inv.armor.set(3 - loc.a(), s); }
                    };
                    case 4 -> new Ref() {
                        public ItemStack get() { return data.rig; }
                        public void set(ItemStack s) { data.rig = s; }
                    };
                    case 5 -> new Ref() {
                        public ItemStack get() { return data.backpack; }
                        public void set(ItemStack s) { data.backpack = s; }
                    };
                    case 6 -> new Ref() {
                        public ItemStack get() { return inv.offhand.get(0); }
                        public void set(ItemStack s) { inv.offhand.set(0, s); }
                    };
                    default -> null;
                };
            }
            case Loc.GRID -> {
                if (loc.a() < 0 || loc.a() >= GridId.values().length) {
                    return null;
                }
                Grid g = grid(player, data, GridId.values()[loc.a()]);
                Placed p = g == null ? null : g.at(loc.b(), loc.c());
                if (p == null) {
                    return null;
                }
                return new Ref() {
                    public ItemStack get() { return p.stack; }
                    public void set(ItemStack s) {
                        if (s.isEmpty()) {
                            g.items.remove(p);
                        } else {
                            p.stack = s;
                        }
                    }
                };
            }
            case Loc.CONTAINER -> {
                List<Shown> shown = containers(player);
                if (loc.a() < 0 || loc.a() >= shown.size()) {
                    return null;
                }
                Container c = shown.get(loc.a()).container;
                ContainerGrid g = shown.get(loc.a()).grid();
                // b = -1: an overflow item, by raw slot (c). Otherwise the grid cell (b, c).
                int slot = loc.b() == -1 ? loc.c() : g.slotAt(loc.b(), loc.c());
                if (slot < 0 || slot >= c.getContainerSize() || c.getItem(slot).isEmpty()) {
                    return null;
                }
                return new Ref() {
                    public ItemStack get() { return c.getItem(slot); }
                    public void set(ItemStack s) {
                        c.setItem(slot, s);
                        if (s.isEmpty()) {
                            g.forget(slot);
                        }
                        g.save();
                        c.setChanged();
                    }
                };
            }
            case Loc.UNSORTED -> {
                if (loc.a() < 0 || loc.a() >= data.unsorted.size()) {
                    return null;
                }
                return new Ref() {
                    public ItemStack get() { return data.unsorted.get(loc.a()); }
                    public void set(ItemStack s) {
                        if (s.isEmpty()) {
                            data.unsorted.remove(loc.a());
                        } else {
                            data.unsorted.set(loc.a(), s);
                        }
                    }
                };
            }
            default -> {
                return null;
            }
        }
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Can this stack go into a single-item location at all? */
    private static boolean accepts(ServerPlayer player, Loc loc, ItemStack stack) {
        if (loc.kind() != Loc.EQUIP) {
            return true;
        }
        return switch (loc.a()) {
            case 0, 1, 2, 3 -> player.getEquipmentSlotForItem(stack) == ARMOR[loc.a()];
            case 4 -> stack.getItem() instanceof GearItem g && g.spec.kind == GearSpec.Kind.RIG;
            case 5 -> stack.getItem() instanceof GearItem g && g.spec.kind == GearSpec.Kind.BACKPACK;
            default -> true;
        };
    }

    /** Can the item at this location be taken out? A rig or backpack must be empty; cursed armor stays on. */
    private static String locked(ServerPlayer player, InvData data, Loc loc, ItemStack stack) {
        if (loc.kind() != Loc.EQUIP) {
            return null;
        }
        if (loc.a() == 4 && !data.rigItems.isEmpty()) {
            return "Empty the rig first.";
        }
        if (loc.a() == 5 && !data.backpackItems.isEmpty()) {
            return "Empty the backpack first.";
        }
        if (loc.a() <= 3 && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return "It won't come off.";
        }
        return null;
    }

    private static int slotLimit(Loc loc, ItemStack stack, Container container) {
        if (loc.kind() == Loc.EQUIP && loc.a() != 6) {
            return 1;
        }
        int max = stack.getMaxStackSize();
        return container != null ? Math.min(max, container.getMaxStackSize()) : max;
    }

    /** Puts a stack at a location. Returns what's left. {@code self} is the grid entry being moved, if any. */
    private static ItemStack insert(ServerPlayer player, InvData data, Loc to, ItemStack stack, boolean rotated, Placed self) {
        switch (to.kind()) {
            case Loc.GRID -> {
                if (to.a() < 0 || to.a() >= GridId.values().length) {
                    return stack;
                }
                GridId id = GridId.values()[to.a()];
                Grid g = grid(player, data, id);
                if (g == null || (stack.getItem() instanceof GearItem gear
                        && ((id == GridId.RIG && gear.spec.kind == GearSpec.Kind.RIG) || (id == GridId.BACKPACK && gear.spec.kind == GearSpec.Kind.BACKPACK)))) {
                    return stack;
                }
                Placed there = g.at(to.b(), to.c());
                if (there != null && there != self) {
                    if (ItemStack.isSameItemSameComponents(there.stack, stack)) {
                        int n = Math.min(stack.getCount(), there.stack.getMaxStackSize() - there.stack.getCount());
                        there.stack.grow(n);
                        stack.shrink(n);
                    }
                    return stack;
                }
                int[] s = ItemSize.of(stack);
                int w = rotated ? s[1] : s[0];
                int h = rotated ? s[0] : s[1];
                if (g.fits(w, h, to.b(), to.c(), self)) {
                    g.items.add(new Placed(stack.copyAndClear(), to.b(), to.c(), rotated && s[0] != s[1]));
                }
                return stack;
            }
            case Loc.DISCARD -> {
                player.drop(stack.copyAndClear(), false, true);
                return stack;
            }
            case Loc.CONTAINER -> {
                List<Shown> shown = containers(player);
                if (to.a() < 0 || to.a() >= shown.size() || to.b() < 0) {
                    return stack;
                }
                ContainerGrid g = shown.get(to.a()).grid();
                g.insertAt(stack, to.b(), to.c(), rotated);
                g.save();
                g.container.setChanged();
                return stack;
            }
            case Loc.HAND, Loc.EQUIP -> {
                Container container = null;
                if (!accepts(player, to, stack)) {
                    return stack;
                }
                if (to.kind() == Loc.EQUIP && (to.a() == 4 || to.a() == 5) && !ref(player, data, to).get().isEmpty()) {
                    return stack;
                }
                Ref r = ref(player, data, to);
                if (r == null) {
                    return stack;
                }
                ItemStack cur = r.get();
                int limit = slotLimit(to, stack, container);
                if (cur.isEmpty()) {
                    r.set(stack.split(Math.min(limit, stack.getCount())));
                } else if (ItemStack.isSameItemSameComponents(cur, stack)) {
                    int n = Math.min(stack.getCount(), limit - cur.getCount());
                    if (n > 0) {
                        cur.grow(n);
                        stack.shrink(n);
                        r.set(cur);
                    }
                }
                return stack;
            }
            default -> {
                return stack;
            }
        }
    }

    private static boolean slotKind(Loc loc) {
        return loc.kind() == Loc.HAND || (loc.kind() == Loc.EQUIP && loc.a() != 4 && loc.a() != 5);
    }

    static void move(ServerPlayer player, Loc from, Loc to, boolean rotated, int count) {
        if (!player.isAlive() || !active(player)) {
            return;
        }
        InvData data = data(player);
        Ref src = ref(player, data, from);
        if (src == null || src.get().isEmpty()) {
            return;
        }
        ItemStack source = src.get();
        String why = locked(player, data, from, source);
        if (why != null) {
            player.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
            return;
        }
        int n = count <= 0 ? source.getCount() : Math.min(count, source.getCount());
        boolean whole = n == source.getCount();
        if (whole && from.kind() == Loc.CONTAINER && to.kind() == Loc.CONTAINER && from.a() == to.a() && from.b() >= 0 && to.b() >= 0) {
            // Rearranging inside one container: just move it in the grid.
            List<Shown> shown = containers(player);
            ContainerGrid g = shown.get(from.a()).grid();
            int slot = g.slotAt(from.b(), from.c());
            int there = g.slotAt(to.b(), to.c());
            if (slot >= 0 && (there < 0 || there == slot)) {
                if (g.relocate(slot, to.b(), to.c(), rotated)) {
                    g.save();
                }
                rt(player).dirty = true;
                return;
            }
        }
        Placed self = null;
        if (whole && from.kind() == Loc.GRID) {
            self = grid(player, data, GridId.values()[from.a()]).at(from.b(), from.c());
        }
        ItemStack moving = source.copyWithCount(n);
        ItemStack left = insert(player, data, to, moving, rotated, self);
        int moved = n - left.getCount();
        if (moved > 0) {
            source.shrink(moved);
            src.set(source.isEmpty() ? ItemStack.EMPTY : source);
        } else if (whole && slotKind(from) && slotKind(to)) {
            // Swap two single-item places, if each can take the other's item.
            Ref dst = ref(player, data, to);
            if (dst != null && !dst.get().isEmpty() && locked(player, data, to, dst.get()) == null
                    && accepts(player, to, source) && accepts(player, from, dst.get())
                    && source.getCount() <= slotLimit(to, source, null) && dst.get().getCount() <= slotLimit(from, dst.get(), null)) {
                ItemStack other = dst.get();
                dst.set(source);
                src.set(other);
            }
        }
        rt(player).dirty = true;
    }

    static void quickMove(ServerPlayer player, Loc from) {
        if (!player.isAlive() || !active(player)) {
            return;
        }
        InvData data = data(player);
        Ref src = ref(player, data, from);
        if (src == null || src.get().isEmpty() || locked(player, data, from, src.get()) != null) {
            return;
        }
        ItemStack stack = src.get().copy();
        if (from.kind() == Loc.CONTAINER) {
            storeInGrids(player, data, stack);
            if (!stack.isEmpty()) {
                freeHotbar(player.getInventory(), stack);
            }
        } else {
            List<Shown> shown = containers(player);
            if (!shown.isEmpty()) {
                for (Shown c : shown) {
                    storeIn(c, stack);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            } else if (from.kind() == Loc.GRID || from.kind() == Loc.UNSORTED) {
                // Equip it if it's wearable and the place is free, else to the hotbar.
                for (int slot = 0; slot < 7 && !stack.isEmpty(); slot++) {
                    Loc eq = Loc.equip(slot);
                    if (slot != 6 && accepts(player, eq, stack) && ref(player, data, eq).get().isEmpty()) {
                        stack = insert(player, data, eq, stack, false, null);
                    }
                }
                if (!stack.isEmpty()) {
                    freeHotbar(player.getInventory(), stack);
                }
            } else {
                storeInGrids(player, data, stack);
            }
        }
        src.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        rt(player).dirty = true;
    }

    static void action(ServerPlayer player, int action) {
        Runtime rt = rt(player);
        InvData data = data(player);
        switch (action) {
            case 0 -> {
                List<ItemStack> left = new ArrayList<>();
                for (GridId id : GridId.values()) {
                    Grid g = grid(player, data, id);
                    if (g != null) {
                        left.addAll(g.repack());
                    }
                }
                left.addAll(data.unsorted);
                data.unsorted.clear();
                for (ItemStack s : left) {
                    storeInGrids(player, data, s);
                    if (!s.isEmpty()) {
                        data.unsorted.add(s);
                    }
                }
            }
            case 1 -> {
                List<Shown> shown = containers(player);
                for (GridId id : AUTO) {
                    for (Placed p : data.list(id)) {
                        for (Shown c : shown) {
                            storeIn(c, p.stack);
                            if (p.stack.isEmpty()) {
                                break;
                            }
                        }
                    }
                }
                purge(data);
            }
            case 2 -> {
                for (ItemStack s : data.unsorted) {
                    player.drop(s, false, true);
                }
                data.unsorted.clear();
            }
            case 3 -> rt.container = null;
            case 4 -> rt.open = true;
            case 5 -> {
                rt.open = false;
                if (rt.container != null) {
                    BlockState state = player.level().getBlockState(rt.container);
                    player.level().playSound(null, rt.container, state.getBlock() instanceof BarrelBlock ? SoundEvents.BARREL_CLOSE
                            : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, player.level().random.nextFloat() * 0.1f + 0.9f);
                }
                rt.container = null;
            }
            case 6 -> openCraftingGrid(player);
            default -> { }
        }
        rt.dirty = true;
    }

    /**
     * A real crafting grid: 3x3 near a crafting table or in the hideout, else the vanilla 2x2 inventory screen.
     * Either way the grid panel shows beside it.
     */
    private static void openCraftingGrid(ServerPlayer player) {
        if (!nearTable(player)) {
            ServerPlayNetworking.send(player, new TetrisNet.Open(-1));
            return;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                new net.minecraft.world.inventory.CraftingMenu(id, inventory,
                        net.minecraft.world.inventory.ContainerLevelAccess.create(player.level(), player.blockPosition())) {
                    @Override
                    public boolean stillValid(Player who) {
                        return who.isAlive();
                    }
                }, Component.translatable("container.crafting")));
    }

    // ================================================================== grid panel beside vanilla screens

    /**
     * The panel works through the open menu's cursor, so the vanilla screen does the rest: click a grid item to
     * pick it up, click a grid cell while holding something to put it there.
     */
    static void panelClick(ServerPlayer player, Loc loc, boolean rotated, int button) {
        if (!player.isAlive() || !active(player) || loc.kind() != Loc.GRID) {
            return;
        }
        var menu = player.containerMenu;
        InvData data = data(player);
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            Ref src = ref(player, data, loc);
            if (src == null || src.get().isEmpty()) {
                return;
            }
            ItemStack stack = src.get();
            if (button == 2) {
                // Shift: straight into the menu's own slots (furnace input, chest, crafting grid...).
                for (var slot : menu.slots) {
                    if (stack.isEmpty()) {
                        break;
                    }
                    if (slot.container instanceof Inventory || !slot.mayPlace(stack)) {
                        continue;
                    }
                    ItemStack in = slot.getItem();
                    int limit = slot.getMaxStackSize(stack);
                    if (in.isEmpty()) {
                        slot.setByPlayer(stack.split(Math.min(limit, stack.getCount())));
                    } else if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < limit) {
                        int n = Math.min(stack.getCount(), limit - in.getCount());
                        in.grow(n);
                        stack.shrink(n);
                        slot.setChanged();
                    }
                }
                src.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
            } else {
                int n = button == 1 ? (stack.getCount() + 1) / 2 : stack.getCount();
                menu.setCarried(stack.split(n));
                src.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        } else {
            ItemStack moving = button == 1 ? carried.copyWithCount(1) : carried.copy();
            int offered = moving.getCount();
            // insert() empties what it places from the stack it's given, so count from before.
            int placed = offered - insert(player, data, loc, moving, rotated, null).getCount();
            carried.shrink(placed);
            menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        }
        menu.broadcastChanges();
        rt(player).dirty = true;
    }

    // ================================================================== death

    /**
     * Raid death rules for the grids. {@code dropAll}: everything drops (outside a raid, or the vanilla rule).
     * The pouch is only lost under the vanilla rule.
     */
    public static void onDeath(ServerPlayer player, boolean raid, Cfg.DeathRule rule) {
        InvData data = data(player);
        if (!raid && player.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        boolean wipe = raid && rule == Cfg.DeathRule.LOSE_EVERYTHING;
        boolean pouchToo = raid && rule == Cfg.DeathRule.VANILLA;
        List<ItemStack> lost = new ArrayList<>();
        for (GridId id : AUTO) {
            data.list(id).forEach(p -> lost.add(p.stack));
            data.list(id).clear();
        }
        lost.addAll(data.unsorted);
        data.unsorted.clear();
        lost.add(data.rig);
        lost.add(data.backpack);
        data.rig = ItemStack.EMPTY;
        data.backpack = ItemStack.EMPTY;
        if (pouchToo) {
            data.pouch.forEach(p -> lost.add(p.stack));
            data.pouch.clear();
        }
        if (!wipe) {
            for (ItemStack s : lost) {
                if (!s.isEmpty()) {
                    player.drop(s, true, false);
                }
            }
        }
        rt(player).dirty = true;
    }

    // ================================================================== crafting

    private static boolean nearTable(ServerPlayer player) {
        if (Hideouts.at(player.serverLevel(), player.position()) != null) {
            return true;
        }
        BlockPos at = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-TABLE_RANGE, -3, -TABLE_RANGE), at.offset(TABLE_RANGE, 3, TABLE_RANGE))) {
            if (player.serverLevel().getBlockState(p).is(Blocks.CRAFTING_TABLE)) {
                return true;
            }
        }
        return false;
    }

    /** What crafting may draw from: hotbar, grids, and the stash when home. */
    private static List<ItemStack> pool(ServerPlayer player, List<Container> touched) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            out.add(inv.items.get(i));
        }
        InvData data = data(player);
        for (GridId id : GridId.values()) {
            data.list(id).forEach(p -> out.add(p.stack));
        }
        Hideouts.Hideout home = Hideouts.at(player.serverLevel(), player.position());
        if (home != null) {
            for (Container c : Stash.containers(player.serverLevel(), home)) {
                touched.add(c);
                for (int i = 0; i < c.getContainerSize(); i++) {
                    out.add(c.getItem(i));
                }
            }
        }
        return out;
    }

    private static boolean usable(CraftingRecipe recipe, boolean table) {
        if (recipe instanceof ShapedRecipe shaped) {
            return table || (shaped.getWidth() <= 2 && shaped.getHeight() <= 2);
        }
        if (recipe instanceof ShapelessRecipe) {
            return table || recipe.getIngredients().size() <= 4;
        }
        return false;
    }

    /** Picks stacks for one craft, adding to {@code used}. Null if the pool can't cover it. */
    private static Map<ItemStack, Integer> match(CraftingRecipe recipe, List<ItemStack> pool, Map<ItemStack, Integer> used) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (Ingredient ing : recipe.getIngredients()) {
            if (!ing.isEmpty()) {
                ingredients.add(ing);
            }
        }
        // Narrow ingredients first, so a flexible one doesn't take what a strict one needs.
        ingredients.sort((a, b) -> Integer.compare(a.getItems().length, b.getItems().length));
        Map<ItemStack, Integer> take = new IdentityHashMap<>();
        for (Ingredient ing : ingredients) {
            boolean found = false;
            for (ItemStack s : pool) {
                int usedHere = used.getOrDefault(s, 0) + take.getOrDefault(s, 0);
                if (!s.isEmpty() && s.getCount() > usedHere && ing.test(s)) {
                    take.merge(s, 1, Integer::sum);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        take.forEach((s, n) -> used.merge(s, n, Integer::sum));
        return take;
    }

    static List<TetrisNet.CraftEntry> craftable(ServerPlayer player) {
        List<TetrisNet.CraftEntry> out = new ArrayList<>();
        if (!active(player)) {
            return out;
        }
        boolean table = nearTable(player);
        List<ItemStack> pool = pool(player, new ArrayList<>());
        var registries = player.registryAccess();
        for (RecipeHolder<CraftingRecipe> holder : player.server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (!usable(recipe, table)) {
                continue;
            }
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty()) {
                continue;
            }
            Map<ItemStack, Integer> used = new IdentityHashMap<>();
            int cap = Math.max(1, result.getMaxStackSize() / result.getCount());
            int times = 0;
            while (times < cap && match(recipe, pool, used) != null) {
                times++;
            }
            if (times > 0) {
                out.add(new TetrisNet.CraftEntry(holder.id().toString(), result.copy(), times));
            }
        }
        out.sort((a, b) -> a.result().getHoverName().getString().compareToIgnoreCase(b.result().getHoverName().getString()));
        return out;
    }

    static void craft(ServerPlayer player, String id, int times) {
        if (!player.isAlive() || !active(player)) {
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return;
        }
        var holder = player.server.getRecipeManager().byKey(key).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe) || !usable(recipe, nearTable(player))) {
            return;
        }
        List<Container> touched = new ArrayList<>();
        List<ItemStack> pool = pool(player, touched);
        ItemStack result = recipe.getResultItem(player.registryAccess());
        int made = 0;
        for (int t = 0; t < Math.max(1, Math.min(64, times)); t++) {
            Map<ItemStack, Integer> take = match(recipe, pool, new IdentityHashMap<>());
            if (take == null) {
                break;
            }
            List<ItemStack> remainders = new ArrayList<>();
            take.forEach((s, n) -> {
                if (s.getItem().hasCraftingRemainingItem()) {
                    for (int i = 0; i < n; i++) {
                        remainders.add(new ItemStack(s.getItem().getCraftingRemainingItem()));
                    }
                }
                s.shrink(n);
            });
            ItemStack out = result.copy();
            out.onCraftedBy(player.level(), player, out.getCount());
            give(player, out);
            remainders.forEach(r -> give(player, r));
            made++;
        }
        touched.forEach(Container::setChanged);
        purge(data(player));
        if (made > 0) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_CRAFTED.get(result.getItem()), made * result.getCount());
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 0.4f, 1.2f);
        }
        rt(player).dirty = true;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        pickup(player, stack);
        if (!stack.isEmpty()) {
            InvData data = data(player);
            data.unsorted.add(stack);
        }
    }
}
