package com.cavmkii.goblincraft.rpg;

/** The six regions of the passive web, clockwise from the top. */
public enum Archetype {
    WARRIOR("Warrior", Attr.STR, Attr.STR, 0xFFD9534F),
    DUELIST("Duelist", Attr.STR, Attr.AGI, 0xFFE08E3C),
    RANGER("Ranger", Attr.AGI, Attr.AGI, 0xFF5CB85C),
    SHADOW("Shadow", Attr.AGI, Attr.INT, 0xFF3FB0A8),
    MAGE("Mage", Attr.INT, Attr.INT, 0xFF428BCA),
    TEMPLAR("Templar", Attr.INT, Attr.STR, 0xFFB57EDC);

    public final String label;
    public final Attr first;
    public final Attr second;
    public final int color;

    Archetype(String label, Attr first, Attr second, int color) {
        this.label = label;
        this.first = first;
        this.second = second;
        this.color = color;
    }

    public boolean hybrid() {
        return first != second;
    }

    public Archetype next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Direction of this region's axis on the web, in radians (0 = up, clockwise). */
    public double angle() {
        return Math.toRadians(ordinal() * 60);
    }
}
