package com.cavmkii.goblincraft.tarkov;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The secure container screen: its grid on top, your inventory below. */
public class SecureMenu extends AbstractContainerMenu {
    public final SecureTier tier;
    private final Container container;

    /** Client side: the tier arrives with the open packet. */
    public SecureMenu(int id, Inventory inventory, Integer tierOrdinal) {
        this(id, inventory, SecureTier.values()[Math.max(0, Math.min(SecureTier.values().length - 1, tierOrdinal))],
                new SimpleContainer(SecureTier.values()[Math.max(0, Math.min(SecureTier.values().length - 1, tierOrdinal))].slots()));
    }

    SecureMenu(int id, Inventory inventory, SecureTier tier, Container container) {
        super(TarkovContent.SECURE_MENU, id);
        this.tier = tier;
        this.container = container;
        int left = 8 + (162 - tier.width * 18) / 2;
        for (int row = 0; row < tier.height; row++) {
            for (int col = 0; col < tier.width; col++) {
                addSlot(new Slot(container, row * tier.width + col, left + col * 18, 18 + row * 18));
            }
        }
        int invTop = topHeight(tier);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, invTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, invTop + 58));
        }
    }

    public static int topHeight(SecureTier tier) {
        return 18 + tier.height * 18 + 14;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int secure = tier.slots();
        if (index < secure) {
            if (!moveItemStackTo(stack, secure, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, secure, false)) {
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
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    /** A container view over the first N slots of the player's saved secure list. */
    static final class View implements Container {
        private final NonNullList<ItemStack> items;
        private final int size;

        View(NonNullList<ItemStack> items, int size) {
            this.items = items;
            this.size = size;
        }

        @Override public int getContainerSize() { return size; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < size; i++) {
                if (!items.get(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
        @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() {
            for (int i = 0; i < size; i++) {
                items.set(i, ItemStack.EMPTY);
            }
        }
    }
}
