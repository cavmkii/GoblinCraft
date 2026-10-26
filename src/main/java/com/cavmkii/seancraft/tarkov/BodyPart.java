package com.cavmkii.seancraft.tarkov;

/** Tarkov's seven body parts and their share of total health (35/85/70/60/60/65/65 of 440). */
public enum BodyPart {
    HEAD("Head", 35),
    THORAX("Thorax", 85),
    STOMACH("Stomach", 70),
    LEFT_ARM("Left arm", 60),
    RIGHT_ARM("Right arm", 60),
    LEFT_LEG("Left leg", 65),
    RIGHT_LEG("Right leg", 65);

    public final String label;
    public final float share;

    BodyPart(String label, int points) {
        this.label = label;
        this.share = points / 440f;
    }

    public boolean limb() {
        return this.ordinal() >= LEFT_ARM.ordinal();
    }

    public boolean arm() {
        return this == LEFT_ARM || this == RIGHT_ARM;
    }

    public boolean leg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }
}
