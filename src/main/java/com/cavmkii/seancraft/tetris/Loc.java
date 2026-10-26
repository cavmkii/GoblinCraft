package com.cavmkii.seancraft.tetris;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Where an item is on the GEAR tab. HAND: hotbar slot. GRID: grid and cell. EQUIP: 0-3 armor (head to feet),
 * 4 rig, 5 backpack, 6 off-hand. CONTAINER: open container (or stash chest) index and grid cell, or -1 and a raw slot
 * for overflow. DISCARD: the ground.
 * UNSORTED: index in the unsorted list.
 */
public record Loc(int kind, int a, int b, int c) {
    public static final int HAND = 0;
    public static final int GRID = 1;
    public static final int EQUIP = 2;
    public static final int CONTAINER = 3;
    public static final int DISCARD = 4;
    public static final int UNSORTED = 5;

    public static Loc hand(int slot) { return new Loc(HAND, slot, 0, 0); }
    public static Loc grid(GridId id, int x, int y) { return new Loc(GRID, id.ordinal(), x, y); }
    public static Loc equip(int slot) { return new Loc(EQUIP, slot, 0, 0); }
    public static Loc container(int index, int x, int y) { return new Loc(CONTAINER, index, x, y); }
    public static Loc containerOverflow(int index, int slot) { return new Loc(CONTAINER, index, -1, slot); }
    public static Loc discard() { return new Loc(DISCARD, 0, 0, 0); }
    public static Loc unsorted(int index) { return new Loc(UNSORTED, index, 0, 0); }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeVarInt(a);
        buf.writeVarInt(b);
        buf.writeVarInt(c);
    }

    public static Loc read(FriendlyByteBuf buf) {
        return new Loc(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
}
