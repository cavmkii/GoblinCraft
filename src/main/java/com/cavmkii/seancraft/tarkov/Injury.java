package com.cavmkii.seancraft.tarkov;

/**
 * What a player's injuries do, worked out the same way on the server (to apply) and the client (to show). Each
 * limb has a severity from 0 to 1: its fracture, half its lasting damage, or 1 if it's destroyed. Two bad legs
 * count for more than one, up to 1. Penalties are fractions (-0.25 is 25% less); aim is extra spread in degrees.
 * Painkillers let you push through: legs count for 30% and arms for 50% while they last, and you can always sprint.
 */
public record Injury(float legs, float arms, float bleed, boolean pain, float move, float sprint, boolean noSprint, float jump,
        float damage, float attackSpeed, float mining, float aim) {
    public static final Injury NONE = new Injury(0, 0, 0, false, 0, 0, false, 0, 0, 0, 0, 0);

    public static Injury of(TarkovData d) {
        return of(d.body, d.fracture, d.scar, d.bleed, d.painkillerTicks);
    }

    public static Injury of(float[] body, float[] fracture, float[] scar, float[] bleed, int painkillerTicks) {
        float legs = pair(limb(body, fracture, scar, BodyPart.LEFT_LEG), limb(body, fracture, scar, BodyPart.RIGHT_LEG));
        float arms = pair(limb(body, fracture, scar, BodyPart.LEFT_ARM), limb(body, fracture, scar, BodyPart.RIGHT_ARM));
        float bled = 0;
        for (float b : bleed) {
            bled += b;
        }
        bled = Math.min(1, bled);
        boolean pain = painkillerTicks > 0;
        float le = pain ? legs * 0.3f : legs;
        float ae = pain ? arms * 0.5f : arms;
        return new Injury(legs, arms, bled, pain,
                -0.40f * le,
                -0.30f * le,
                !pain && legs >= 0.999f,
                -0.10f * le,
                -0.40f * ae,
                -0.30f * ae,
                -Math.min(0.7f, 0.40f * ae + 0.20f * le + 0.15f * bled),
                10f * ae);
    }

    /** One limb's severity. */
    public static float limb(float[] body, float[] fracture, float[] scar, BodyPart p) {
        int i = p.ordinal();
        return body[i] <= 0.0001f ? 1 : Math.max(fracture[i], scar[i] * 0.5f);
    }

    private static float pair(float a, float b) {
        return Math.min(1, Math.max(a, b) + 0.5f * Math.min(a, b));
    }

    /** Legs bad enough to walk unevenly, as 0-1 for the limp. */
    public float limp() {
        return pain ? legs * 0.3f : legs;
    }

    public boolean any() {
        return legs > 0.001f || arms > 0.001f || bleed > 0.001f;
    }
}
