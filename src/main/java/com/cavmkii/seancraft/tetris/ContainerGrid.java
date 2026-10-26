package com.cavmkii.seancraft.tetris;

import com.cavmkii.seancraft.SeanCraft;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A chest or barrel shown as a Tetris grid. The items stay in the container's ordinary slots (so hoppers and other
 * mods still work); only where each slot's item sits in the grid is stored, on the block entity. Items added from
 * outside are placed automatically; any that can't fit are listed as overflow.
 */
public final class ContainerGrid {
    /** Slot index (as a string) to [x, y, rotated]. */
    public static final AttachmentType<Map<String, List<Integer>>> LAYOUT = AttachmentRegistry.<Map<String, List<Integer>>>builder()
            .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT.listOf()))
            .initializer(HashMap::new)
            .buildAndRegister(SeanCraft.id("container_layout"));

    public static final int WIDTH = 9;

    public final Container container;
    private final BlockEntity holder;
    public final int width;
    public final int height;
    /** Slot to [x, y, rotated]. */
    private final Map<Integer, int[]> pos = new HashMap<>();
    public final List<Integer> overflow = new ArrayList<>();

    private ContainerGrid(Container container, BlockEntity holder) {
        this.container = container;
        this.holder = holder;
        this.width = WIDTH;
        this.height = container.getContainerSize() / WIDTH + 2;
    }

    static void init() {
        // Loading the class registers the attachment.
    }

    /** Loads the saved layout, drops entries that no longer fit, and places anything new. */
    public static ContainerGrid of(Container container, BlockEntity holder) {
        ContainerGrid g = new ContainerGrid(container, holder);
        Map<String, List<Integer>> saved = holder.getAttachedOrCreate(LAYOUT);
        List<Integer> unplaced = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            List<Integer> p = saved.get(String.valueOf(slot));
            int[] size = ItemSize.of(stack);
            if (p != null && p.size() == 3) {
                boolean rot = p.get(2) != 0;
                int w = rot ? size[1] : size[0];
                int h = rot ? size[0] : size[1];
                if (g.fits(w, h, p.get(0), p.get(1), -1)) {
                    g.pos.put(slot, new int[] {p.get(0), p.get(1), rot ? 1 : 0});
                    continue;
                }
            }
            unplaced.add(slot);
        }
        for (int slot : unplaced) {
            if (!g.autoPlace(slot)) {
                g.overflow.add(slot);
            }
        }
        g.save();
        return g;
    }

    public void save() {
        Map<String, List<Integer>> out = new HashMap<>();
        pos.forEach((slot, p) -> out.put(String.valueOf(slot), List.of(p[0], p[1], p[2])));
        holder.setAttached(LAYOUT, out);
        holder.setChanged();
    }

    private int[] footprint(int slot) {
        int[] size = ItemSize.of(container.getItem(slot));
        int[] p = pos.get(slot);
        return p != null && p[2] != 0 ? new int[] {size[1], size[0]} : size;
    }

    /** The slot whose item covers a cell, or -1. */
    public int slotAt(int cx, int cy) {
        for (Map.Entry<Integer, int[]> e : pos.entrySet()) {
            int[] p = e.getValue();
            int[] f = footprint(e.getKey());
            if (cx >= p[0] && cx < p[0] + f[0] && cy >= p[1] && cy < p[1] + f[1]) {
                return e.getKey();
            }
        }
        return -1;
    }

    public boolean fits(int w, int h, int x, int y, int ignoreSlot) {
        if (x < 0 || y < 0 || x + w > width || y + h > height) {
            return false;
        }
        for (Map.Entry<Integer, int[]> e : pos.entrySet()) {
            if (e.getKey() == ignoreSlot) {
                continue;
            }
            int[] p = e.getValue();
            int[] f = footprint(e.getKey());
            if (x < p[0] + f[0] && x + w > p[0] && y < p[1] + f[1] && y + h > p[1]) {
                return false;
            }
        }
        return true;
    }

    private boolean autoPlace(int slot) {
        int[] s = ItemSize.of(container.getItem(slot));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (fits(s[0], s[1], x, y, -1)) {
                    pos.put(slot, new int[] {x, y, 0});
                    return true;
                }
                if (s[0] != s[1] && fits(s[1], s[0], x, y, -1)) {
                    pos.put(slot, new int[] {x, y, 1});
                    return true;
                }
            }
        }
        return false;
    }

    private int freeSlot(ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty() && container.canPlaceItem(i, stack)) {
                return i;
            }
        }
        return -1;
    }

    private int limit(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
    }

    /** Puts a stack with its top-left at a cell, or tops up the same item there. Returns what's left. */
    public ItemStack insertAt(ItemStack stack, int x, int y, boolean rotated) {
        int there = slotAt(x, y);
        if (there >= 0) {
            ItemStack in = container.getItem(there);
            if (ItemStack.isSameItemSameComponents(in, stack)) {
                int n = Math.min(stack.getCount(), limit(in) - in.getCount());
                if (n > 0) {
                    in.grow(n);
                    stack.shrink(n);
                }
            }
            return stack;
        }
        int[] s = ItemSize.of(stack);
        boolean rot = rotated && s[0] != s[1];
        int w = rot ? s[1] : s[0];
        int h = rot ? s[0] : s[1];
        int slot = freeSlot(stack);
        if (slot >= 0 && fits(w, h, x, y, -1)) {
            container.setItem(slot, stack.split(Math.min(limit(stack), stack.getCount())));
            pos.put(slot, new int[] {x, y, rot ? 1 : 0});
        }
        return stack;
    }

    /** Tops up matching stacks, then places the rest wherever it fits. Leaves what didn't fit in the stack. */
    public ItemStack insertAuto(ItemStack stack) {
        for (int slot : new ArrayList<>(pos.keySet())) {
            ItemStack in = container.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(in, stack)) {
                int n = Math.min(stack.getCount(), limit(in) - in.getCount());
                if (n > 0) {
                    in.grow(n);
                    stack.shrink(n);
                }
            }
        }
        while (!stack.isEmpty()) {
            int slot = freeSlot(stack);
            if (slot < 0) {
                break;
            }
            container.setItem(slot, stack.split(Math.min(limit(stack), stack.getCount())));
            if (!autoPlace(slot)) {
                // Doesn't fit: put it back.
                stack.grow(container.getItem(slot).getCount());
                container.setItem(slot, ItemStack.EMPTY);
                break;
            }
        }
        return stack;
    }

    /** Moves a slot's item to a new top-left cell (and rotation) within this grid. */
    public boolean relocate(int slot, int x, int y, boolean rotated) {
        int[] s = ItemSize.of(container.getItem(slot));
        boolean rot = rotated && s[0] != s[1];
        int w = rot ? s[1] : s[0];
        int h = rot ? s[0] : s[1];
        if (!fits(w, h, x, y, slot)) {
            return false;
        }
        pos.put(slot, new int[] {x, y, rot ? 1 : 0});
        return true;
    }

    /** Merges partial stacks, then repacks everything biggest first. */
    public void sort() {
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack a = container.getItem(i);
            if (a.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < size && a.getCount() < limit(a); j++) {
                ItemStack b = container.getItem(j);
                if (ItemStack.isSameItemSameComponents(a, b)) {
                    int n = Math.min(b.getCount(), limit(a) - a.getCount());
                    a.grow(n);
                    b.shrink(n);
                    if (b.isEmpty()) {
                        container.setItem(j, ItemStack.EMPTY);
                    }
                }
            }
        }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (!container.getItem(i).isEmpty()) {
                slots.add(i);
            }
        }
        slots.sort((x, y) -> {
            int[] sx = ItemSize.of(container.getItem(x));
            int[] sy = ItemSize.of(container.getItem(y));
            int c = Integer.compare(sy[0] * sy[1], sx[0] * sx[1]);
            return c != 0 ? c : container.getItem(x).getItem().toString().compareTo(container.getItem(y).getItem().toString());
        });
        pos.clear();
        overflow.clear();
        for (int slot : slots) {
            if (!autoPlace(slot)) {
                overflow.add(slot);
            }
        }
    }

    public void forget(int slot) {
        pos.remove(slot);
    }

    /** Placed copies for the client. */
    public List<Placed> view() {
        List<Placed> out = new ArrayList<>();
        pos.forEach((slot, p) -> out.add(new Placed(container.getItem(slot).copy(), p[0], p[1], p[2] != 0)));
        return out;
    }
}
