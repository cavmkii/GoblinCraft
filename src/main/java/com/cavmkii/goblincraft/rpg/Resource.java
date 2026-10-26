package com.cavmkii.goblincraft.rpg;

/** What an active skill costs. Rage builds in combat; mana and stamina regenerate. */
public enum Resource {
    RAGE("Rage", 0xFFC0392B),
    STAMINA("Stamina", 0xFFE5B92E),
    MANA("Mana", 0xFF2E7BE5);

    public final String label;
    public final int color;

    Resource(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
