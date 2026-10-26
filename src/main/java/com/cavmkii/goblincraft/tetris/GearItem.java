package com.cavmkii.goblincraft.tetris;

import net.minecraft.world.item.Item;

/** A rig or backpack. It only does something in its equipment slot on the GEAR tab. */
public class GearItem extends Item {
    public final GearSpec spec;

    public GearItem(GearSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }
}
