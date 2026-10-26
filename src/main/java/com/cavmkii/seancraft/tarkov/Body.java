package com.cavmkii.seancraft.tarkov;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Body-part health arithmetic on a player's {@link TarkovData}. Each part's health is a fraction of its share;
 * the parts always add up to the player's real health. A destroyed part (0) doesn't heal by itself.
 */
public final class Body {
    public static final int LIGHT_BLEED = 1;
    public static final int HEAVY_BLEED = 2;
    public static final int FRACTURE = 4;

    private static final float EPS = 0.0001f;

    private Body() {}

    public static boolean destroyed(TarkovData d, BodyPart p) {
        return d.body[p.ordinal()] <= EPS;
    }

    public static boolean has(TarkovData d, BodyPart p, int flag) {
        return (d.conditions[p.ordinal()] & flag) != 0;
    }

    public static boolean hasAny(TarkovData d, int flag) {
        for (int c : d.conditions) {
            if ((c & flag) != 0) {
                return true;
            }
        }
        return false;
    }

    public static void set(TarkovData d, BodyPart p, int flag, boolean on) {
        if (on) {
            d.conditions[p.ordinal()] |= flag;
        } else {
            d.conditions[p.ordinal()] &= ~flag;
        }
    }

    public static void clearAll(TarkovData d, int flag) {
        for (int i = 0; i < d.conditions.length; i++) {
            d.conditions[i] &= ~flag;
        }
    }

    public static void reset(TarkovData d) {
        java.util.Arrays.fill(d.body, 1f);
        java.util.Arrays.fill(d.conditions, 0);
        d.painkillerTicks = 0;
    }

    /** Health the parts add up to, as a fraction of max health. */
    /** Share of max health still reachable: destroyed parts lock theirs away until a surgical kit restores them. */
    public static float capFraction(float[] body) {
        float cap = 1;
        for (BodyPart p : BodyPart.values()) {
            if (body[p.ordinal()] <= EPS) {
                cap -= p.share;
            }
        }
        return Math.max(0, cap);
    }

    public static float total(TarkovData d) {
        float t = 0;
        for (BodyPart p : BodyPart.values()) {
            t += d.body[p.ordinal()] * p.share;
        }
        return t;
    }

    /** Damage one part by an amount of health; anything past 0 spreads to the others. */
    public static void damage(TarkovData d, BodyPart part, float amount, float maxHealth) {
        float frac = amount / Math.max(1, maxHealth);
        int i = part.ordinal();
        float can = d.body[i] * part.share;
        float here = Math.min(frac, can);
        d.body[i] -= here / part.share;
        spread(d, -(frac - here), i);
        clamp(d);
    }

    /** Heal one part. A destroyed part only heals when {@code revive} is set (surgery). Returns health used. */
    public static float heal(TarkovData d, BodyPart part, float amount, float maxHealth, boolean revive) {
        int i = part.ordinal();
        if (d.body[i] <= EPS && !revive) {
            return 0;
        }
        float max = Math.max(1, maxHealth) * part.share;
        float took = Math.min((1 - d.body[i]) * max, amount);
        d.body[i] = Mth.clamp(d.body[i] + took / max, 0, 1);
        if (revive && d.body[i] < 0.05f) {
            d.body[i] = 0.05f;
        }
        return took;
    }

    /** Spread a change (in total-health units) over the eligible parts, by share. */
    public static void spread(TarkovData d, float delta, int except) {
        for (int pass = 0; pass < 4 && Math.abs(delta) > 0.00001f; pass++) {
            float weight = 0;
            for (BodyPart p : BodyPart.values()) {
                if (p.ordinal() != except && eligible(d, p.ordinal(), delta)) {
                    weight += p.share;
                }
            }
            if (weight <= 0) {
                return;
            }
            float left = 0;
            for (BodyPart p : BodyPart.values()) {
                int i = p.ordinal();
                if (i == except || !eligible(d, i, delta)) {
                    continue;
                }
                float want = delta * p.share / weight;
                float before = d.body[i];
                d.body[i] = Mth.clamp(d.body[i] + want / p.share, 0, 1);
                left += want - (d.body[i] - before) * p.share;
            }
            delta = left;
        }
    }

    private static boolean eligible(TarkovData d, int i, float delta) {
        return delta < 0 ? d.body[i] > EPS : d.body[i] > EPS && d.body[i] < 1 - EPS;
    }

    private static void clamp(TarkovData d) {
        for (int i = 0; i < d.body.length; i++) {
            d.body[i] = Mth.clamp(d.body[i], 0, 1);
        }
    }

    /** A part chosen by share, for hits with no useful position. */
    public static BodyPart weighted(RandomSource random) {
        float r = random.nextFloat();
        float acc = 0;
        for (BodyPart p : BodyPart.values()) {
            acc += p.share;
            if (r <= acc) {
                return p;
            }
        }
        return BodyPart.THORAX;
    }
}
