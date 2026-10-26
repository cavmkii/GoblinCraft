package com.cavmkii.goblincraft.tetris;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** A Tetris grid: a size and the items placed in it. */
public final class Grid {
    public final int width;
    public final int height;
    public final List<Placed> items;

    public Grid(int width, int height, List<Placed> items) {
        this.width = width;
        this.height = height;
        this.items = items;
    }

    public Placed at(int cx, int cy) {
        for (Placed p : items) {
            if (p.covers(cx, cy)) {
                return p;
            }
        }
        return null;
    }

    /** Can an item of this footprint sit at (x, y), ignoring one item (the one being moved)? */
    public boolean fits(int w, int h, int x, int y, Placed ignore) {
        if (x < 0 || y < 0 || x + w > width || y + h > height) {
            return false;
        }
        for (Placed p : items) {
            if (p == ignore) {
                continue;
            }
            if (x < p.x + p.width() && x + w > p.x && y < p.y + p.height() && y + h > p.y) {
                return false;
            }
        }
        return true;
    }

    /** First free spot for a stack, trying it upright then rotated. Null if none. */
    public Placed findSpot(ItemStack stack) {
        int[] s = ItemSize.of(stack);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (fits(s[0], s[1], x, y, null)) {
                    return new Placed(stack, x, y, false);
                }
                if (s[0] != s[1] && fits(s[1], s[0], x, y, null)) {
                    return new Placed(stack, x, y, true);
                }
            }
        }
        return null;
    }

    /** Tops up matching stacks in this grid. Returns what's left. */
    public ItemStack merge(ItemStack stack) {
        for (Placed p : items) {
            if (stack.isEmpty()) {
                break;
            }
            if (ItemStack.isSameItemSameComponents(p.stack, stack) && p.stack.getCount() < p.stack.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), p.stack.getMaxStackSize() - p.stack.getCount());
                p.stack.grow(move);
                stack.shrink(move);
            }
        }
        return stack;
    }

    /** Places a whole stack in the first free spot. Returns true if it went in. */
    public boolean add(ItemStack stack) {
        Placed spot = findSpot(stack);
        if (spot == null) {
            return false;
        }
        items.add(spot);
        return true;
    }

    /** Repacks everything, biggest first. Items that no longer fit are returned. */
    public List<ItemStack> repack() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Placed p : items) {
            stacks.add(p.stack);
        }
        items.clear();
        stacks.sort(Comparator.comparingInt((ItemStack s) -> -area(s)).thenComparing(s -> s.getItem().toString()));
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack s : stacks) {
            ItemStack rest = merge(s);
            if (!rest.isEmpty() && !add(rest)) {
                left.add(rest);
            }
        }
        return left;
    }

    private static int area(ItemStack s) {
        int[] size = ItemSize.of(s);
        return size[0] * size[1];
    }
}
