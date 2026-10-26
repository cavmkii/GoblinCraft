package com.cavmkii.goblincraft.parts;

import com.cavmkii.goblincraft.tarkov.Cost;
import com.cavmkii.goblincraft.tarkov.Hideouts;
import com.cavmkii.goblincraft.tarkov.Stash;
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
 * Weapon Bench: put a weapon or armor piece in the left slot and its parts appear in the slots beside it. Take a
 * part out to remove it, put one in to fit it. The arrow buttons level a part up with its own material.
 */
public class WeaponBenchMenu extends AbstractContainerMenu {
    public static final int PARTS = 4;
    public static final int ITEM_X = 20;
    public static final int ITEM_Y = 40;
    public static final int PART_X = 62;
    public static final int PART_Y = 18;

    private final ContainerLevelAccess access;
    private final SimpleContainer item = new SimpleContainer(1);
    private final SimpleContainer parts = new SimpleContainer(PARTS);
    private final boolean server;
    private boolean loading;

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
                return Parts.modular(stack);
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
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 166));
        }
        item.addListener(c -> loadParts());
        parts.addListener(c -> saveParts());
    }

    public ItemStack weapon() {
        return item.getItem(0);
    }

    public Family family() {
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

    /** Button i levels up the part in slot i. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
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
