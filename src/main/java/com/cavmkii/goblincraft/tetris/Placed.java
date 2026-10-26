package com.cavmkii.goblincraft.tetris;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** An item in a grid: its stack, top-left cell, and whether it's turned sideways. */
public final class Placed {
    public ItemStack stack;
    public int x;
    public int y;
    public boolean rotated;

    public Placed(ItemStack stack, int x, int y, boolean rotated) {
        this.stack = stack;
        this.x = x;
        this.y = y;
        this.rotated = rotated;
    }

    public int width() {
        int[] s = ItemSize.of(stack);
        return rotated ? s[1] : s[0];
    }

    public int height() {
        int[] s = ItemSize.of(stack);
        return rotated ? s[0] : s[1];
    }

    public boolean covers(int cx, int cy) {
        return cx >= x && cx < x + width() && cy >= y && cy < y + height();
    }

    public static final Codec<Placed> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("item").forGetter(p -> p.stack),
            Codec.INT.fieldOf("x").forGetter(p -> p.x),
            Codec.INT.fieldOf("y").forGetter(p -> p.y),
            Codec.BOOL.optionalFieldOf("rotated", false).forGetter(p -> p.rotated)
    ).apply(i, Placed::new));

    public void write(RegistryFriendlyByteBuf buf) {
        ItemStack.STREAM_CODEC.encode(buf, stack);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeBoolean(rotated);
    }

    public static Placed read(RegistryFriendlyByteBuf buf) {
        return new Placed(ItemStack.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }
}
