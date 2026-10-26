package com.cavmkii.goblincraft.tarkov;

/** Secure container sizes, Alpha to Kappa. What's inside survives death in raid. */
public enum SecureTier {
    ALPHA("Alpha", 2, 2),
    BETA("Beta", 3, 2),
    GAMMA("Gamma", 3, 3),
    KAPPA("Kappa", 4, 3);

    public final String label;
    public final int width;
    public final int height;

    SecureTier(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public int slots() {
        return width * height;
    }
}
