package com.cavmkii.seancraft.tetris;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * The Tetris inventory: equipped rig and backpack, the contents of each grid, and anything that had nowhere to go
 * (UNSORTED). Kept through death; the death rules decide what survives.
 */
public final class InvData {
    public static final int POCKET_W = 5;
    public static final int POCKET_H = 2;

    public ItemStack rig = ItemStack.EMPTY;
    public ItemStack backpack = ItemStack.EMPTY;
    public final List<Placed> pockets = new ArrayList<>();
    public final List<Placed> rigItems = new ArrayList<>();
    public final List<Placed> backpackItems = new ArrayList<>();
    public final List<Placed> pouch = new ArrayList<>();
    public final List<ItemStack> unsorted = new ArrayList<>();
    public boolean migrated;
    /** Refill a hotbar slot from the grids when its last item gets used up. */
    public boolean autoRefill = true;
    /** ...and from the stash too, when in a hideout. */
    public boolean refillFromStash;

    public static final Codec<InvData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("rig", ItemStack.EMPTY).forGetter(d -> d.rig),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("backpack", ItemStack.EMPTY).forGetter(d -> d.backpack),
            Placed.CODEC.listOf().optionalFieldOf("pockets", List.of()).forGetter(d -> d.pockets),
            Placed.CODEC.listOf().optionalFieldOf("rigItems", List.of()).forGetter(d -> d.rigItems),
            Placed.CODEC.listOf().optionalFieldOf("backpackItems", List.of()).forGetter(d -> d.backpackItems),
            Placed.CODEC.listOf().optionalFieldOf("pouch", List.of()).forGetter(d -> d.pouch),
            ItemStack.CODEC.listOf().optionalFieldOf("unsorted", List.of()).forGetter(d -> d.unsorted),
            Codec.BOOL.optionalFieldOf("migrated", false).forGetter(d -> d.migrated),
            Codec.BOOL.optionalFieldOf("autoRefill", true).forGetter(d -> d.autoRefill),
            Codec.BOOL.optionalFieldOf("refillFromStash", false).forGetter(d -> d.refillFromStash)
    ).apply(i, (rig, backpack, pockets, rigItems, backpackItems, pouch, unsorted, migrated, refill, refillStash) -> {
        InvData d = new InvData();
        d.rig = rig;
        d.backpack = backpack;
        d.pockets.addAll(pockets);
        d.rigItems.addAll(rigItems);
        d.backpackItems.addAll(backpackItems);
        d.pouch.addAll(pouch);
        d.unsorted.addAll(unsorted);
        d.migrated = migrated;
        d.autoRefill = refill;
        d.refillFromStash = refillStash;
        return d;
    }));

    public List<Placed> list(GridId id) {
        return switch (id) {
            case POCKETS -> pockets;
            case RIG -> rigItems;
            case BACKPACK -> backpackItems;
            case POUCH -> pouch;
        };
    }

    /** The grid for an id, sized by the equipped gear and secure tier. Null if that grid doesn't exist right now. */
    public Grid grid(GridId id, int pouchW, int pouchH) {
        return switch (id) {
            case POCKETS -> new Grid(POCKET_W, POCKET_H, pockets);
            case RIG -> rig.getItem() instanceof GearItem g ? new Grid(g.spec.gridWidth, g.spec.gridHeight, rigItems) : null;
            case BACKPACK -> backpack.getItem() instanceof GearItem g ? new Grid(g.spec.gridWidth, g.spec.gridHeight, backpackItems) : null;
            case POUCH -> new Grid(pouchW, pouchH, pouch);
        };
    }
}
