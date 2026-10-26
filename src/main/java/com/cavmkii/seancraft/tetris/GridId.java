package com.cavmkii.seancraft.tetris;

/** The player's grids. */
public enum GridId {
    POCKETS("POCKETS"), RIG("RIG"), BACKPACK("BACKPACK"), POUCH("POUCH");

    public final String label;

    GridId(String label) {
        this.label = label;
    }
}
