package com.cavmkii.seancraft.parts;

import com.cavmkii.seancraft.tarkov.Cost;
import com.cavmkii.seancraft.tarkov.Hideouts;
import com.cavmkii.seancraft.tarkov.Stash;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Weapon Bench. Modify: put a weapon or armor piece in the left slot and its parts appear in the slots beside it;
 * take a part out to remove it, put one in to fit it. Build: with the left slot empty, pick what to make, fill its
 * labelled part slots, and ASSEMBLE. The arrow buttons level a part up with its own material.
 */
public class WeaponBenchMenu extends AbstractContainerMenu {
    public static final int PARTS = 4;
    public static final int ITEM_X = 20;
    public static final int ITEM_Y = 24;
    public static final int PART_X = 62;
    public static final int PART_Y = 18;

    private final ContainerLevelAccess access;
    private final SimpleContainer item = new SimpleContainer(1);
    private final SimpleContainer parts = new SimpleContainer(PARTS);
    private final boolean server;
    private boolean loading;
    /** Family picked to build, as an index into {@link #BUILDABLE}, or -1. Synced to the client. */
    private final net.minecraft.world.inventory.DataSlot building = net.minecraft.world.inventory.DataSlot.standalone();

    /** What the bench can build from scratch. Bows keep their vanilla recipe. */
    public static final Family[] BUILDABLE = {Family.SWORD, Family.AXE, Family.CROSSBOW, Family.SHIELD,
            Family.HELMET, Family.CHEST, Family.LEGS, Family.BOOTS};
    public static final int ASSEMBLE_BUTTON = 200;
    public static final int PICK_BUTTON = 100;

    public WeaponBenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public WeaponBenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(PartItems.BENCH_MENU, id);
        this.access = access;
        this.server = !inventory.player.level().isClientSide;
        addSlot(new Slot(item, 0, ITEM_X, ITEM_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Not while loose parts for a build sit in the part slots: they'd be replaced by the item's own.
                return Parts.modular(stack) && parts.isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < PARTS; i++) {
            // Not "index": that name would resolve to Slot's own index field inside the subclass.
            final int partIndex = i;
            addSlot(new Slot(parts, i, PART_X, PART_Y + i * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    PartSlot slot = slotFor(partIndex);
                    return slot != null && stack.getItem() instanceof PartItem part
                            && family().fits(part.def) && part.def.kind().slot == slot;
                }

                @Override
                public boolean isActive() {
                    return slotFor(partIndex) != null;
                }

                @Override
                public int getMaxStackSize() {
                    return weapon().isEmpty() && slotFor(partIndex) == PartSlot.PLATE ? platesFor(family()) : 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return getMaxStackSize();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 130 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 188));
        }
        item.addListener(c -> loadParts());
        parts.addListener(c -> saveParts());
        building.set(-1);
        addDataSlot(building);
    }

    public static int platesFor(Family family) {
        return switch (family) {
            case HELMET -> 2;
            case CHEST -> 4;
            case LEGS -> 3;
            case BOOTS -> 1;
            default -> 1;
        };
    }

    public boolean buildMode() {
        return weapon().isEmpty();
    }

    public int buildingIndex() {
        return building.get();
    }

    /** What ASSEMBLE would make from the loose parts, or empty. Works on both sides, for the preview. */
    public ItemStack assemblyResult() {
        Family family = family();
        if (!buildMode() || family == null) {
            return ItemStack.EMPTY;
        }
        Map<PartSlot, Parts.Installed> map = new EnumMap<>(PartSlot.class);
        int metal = -1;
        for (int i = 0; i < family.slots.size(); i++) {
            PartSlot slot = family.slots.get(i);
            ItemStack stack = parts.getItem(i);
            if (!(stack.getItem() instanceof PartItem part)) {
                return ItemStack.EMPTY;
            }
            if (slot == PartSlot.PLATE && stack.getCount() < platesFor(family)) {
                return ItemStack.EMPTY;
            }
            if (slot == PartSlot.HEAD || slot == PartSlot.PLATE || (slot == PartSlot.GUARD && family == Family.SWORD)) {
                if (metal >= 0 && metal != part.def.tier()) {
                    return ItemStack.EMPTY; // blade and guard must be the same metal
                }
                metal = part.def.tier();
            }
            map.put(slot, new Parts.Installed(part.def, Parts.level(stack)));
        }
        net.minecraft.world.item.Item result = switch (family) {
            case CROSSBOW -> net.minecraft.world.item.Items.CROSSBOW;
            case SHIELD -> net.minecraft.world.item.Items.SHIELD;
            default -> Family.itemFor(family, metal);
        };
        if (result == null) {
            return ItemStack.EMPTY; // e.g. copper has no sword
        }
        ItemStack out = new ItemStack(result);
        Parts.setInstalled(out, map);
        return out;
    }

    public ItemStack weapon() {
        return item.getItem(0);
    }

    public Family family() {
        if (weapon().isEmpty()) {
            int b = building.get();
            return b >= 0 && b < BUILDABLE.length ? BUILDABLE[b] : null;
        }
        Family.Stock stock = Family.stock(weapon());
        return stock != null ? stock.family() : null;
    }

    public PartSlot slotFor(int index) {
        Family family = family();
        return family != null && index < family.slots.size() ? family.slots.get(index) : null;
    }

    private void loadParts() {
        if (!server) {
            return;
        }
        loading = true;
        try {
            ItemStack w = weapon();
            Map<PartSlot, Parts.Installed> installed = Parts.installed(w);
            if (!w.isEmpty() && w.get(Parts.INSTALLED) == null && Parts.modular(w)) {
                // Loot and old items get their standard parts fitted the first time.
                Parts.setInstalled(w, installed);
            }
            for (int i = 0; i < PARTS; i++) {
                PartSlot slot = slotFor(i);
                Parts.Installed inst = slot != null ? installed.get(slot) : null;
                parts.setItem(i, inst != null ? Parts.partStack(inst) : ItemStack.EMPTY);
            }
        } finally {
            loading = false;
        }
    }

    private void saveParts() {
        if (!server || loading || weapon().isEmpty()) {
            return;
        }
        Map<PartSlot, Parts.Installed> map = new EnumMap<>(PartSlot.class);
        for (int i = 0; i < PARTS; i++) {
            PartSlot slot = slotFor(i);
            ItemStack stack = parts.getItem(i);
            if (slot != null && stack.getItem() instanceof PartItem part) {
                map.put(slot, new Parts.Installed(part.def, Parts.level(stack)));
            }
        }
        Parts.setInstalled(weapon(), map);
    }

    /** Button i levels up the part in slot i; PICK_BUTTON + n picks what to build; ASSEMBLE_BUTTON builds it. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= PICK_BUTTON && id < PICK_BUTTON + BUILDABLE.length && buildMode()) {
            if (building.get() != id - PICK_BUTTON) {
                clearContainer(player, parts); // hand back parts meant for something else
                building.set(id - PICK_BUTTON);
            }
            return true;
        }
        if (id == ASSEMBLE_BUTTON) {
            ItemStack result = assemblyResult();
            if (result.isEmpty()) {
                return false;
            }
            loading = true;
            for (int i = 0; i < PARTS; i++) {
                parts.setItem(i, ItemStack.EMPTY);
            }
            loading = false;
            item.setItem(0, result);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1f);
            return true;
        }
        if (id < 0 || id >= PARTS || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ItemStack stack = parts.getItem(id);
        if (!(stack.getItem() instanceof PartItem part)) {
            return false;
        }
        int level = Parts.level(stack);
        if (level >= PartDef.MAX_LEVEL) {
            return false;
        }
        List<Cost> cost = List.of(Cost.of(part.def.material(), part.def.upgradeCost(level)));
        Hideouts.Hideout hideout = Hideouts.at(serverPlayer.serverLevel(), serverPlayer.position());
        if (!Stash.canAfford(serverPlayer, hideout, cost)) {
            return false;
        }
        Stash.consume(serverPlayer, hideout, cost);
        ItemStack upgraded = stack.copy();
        upgraded.set(Parts.LEVEL, level + 1);
        parts.setItem(id, upgraded);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int benchSlots = 1 + PARTS;
        if (index < benchSlots) {
            if (!moveItemStackTo(stack, benchSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (Parts.modular(stack) && weapon().isEmpty()) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, benchSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (buildMode()) {
            // Loose parts for a build that wasn't assembled go back to the player.
            clearContainer(player, parts);
            return;
        }
        // The parts live inside the item; only the item itself goes back to the player.
        loading = true;
        for (int i = 0; i < PARTS; i++) {
            parts.setItem(i, ItemStack.EMPTY);
        }
        loading = false;
        access.execute((level, pos) -> clearContainer(player, item));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, PartItems.WEAPON_BENCH);
    }
}
