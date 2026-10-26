package com.cavmkii.goblincraft.parts;

import net.minecraft.world.item.Item;

/** A weapon or armor part. Its level lives in the {@link Parts#LEVEL} component. */
public class PartItem extends Item {
    public final PartDef def;

    public PartItem(PartDef def, Properties properties) {
        super(properties);
        this.def = def;
    }
}
