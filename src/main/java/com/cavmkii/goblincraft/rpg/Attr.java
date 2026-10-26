package com.cavmkii.goblincraft.rpg;

/** The three main attributes, Dota-style. */
public enum Attr {
    STR("Strength", 0xE04A3A),
    AGI("Agility", 0x4CC24C),
    INT("Intelligence", 0x4A86E8);

    public final String label;
    public final int color;

    Attr(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
