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
    /** Bleed severity from which it counts as a heavy bleed (needs a tourniquet). */
    public static final float HEAVY = 0.5f;

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

    /**
     * Turns conditions on or off. The severities are what count; the flags follow them. Turning one on without a
     * severity gives it a middling one; turning a bleed off only clears the kind named (light or heavy).
     */
    public static void set(TarkovData d, BodyPart p, int flag, boolean on) {
        int i = p.ordinal();
        if (on) {
            if ((flag & HEAVY_BLEED) != 0) {
                d.bleed[i] = Math.max(d.bleed[i], 0.7f);
            } else if ((flag & LIGHT_BLEED) != 0 && d.bleed[i] <= EPS) {
                d.bleed[i] = 0.3f;
            }
            if ((flag & FRACTURE) != 0) {
                d.fracture[i] = Math.max(d.fracture[i], 0.5f);
            }
        } else {
            if ((flag & LIGHT_BLEED) != 0 && d.bleed[i] < HEAVY || (flag & HEAVY_BLEED) != 0 && d.bleed[i] >= HEAVY) {
                d.bleed[i] = 0;
            }
            if ((flag & FRACTURE) != 0) {
                d.fracture[i] = 0;
            }
        }
        sync(d, i);
    }

    public static void clearAll(TarkovData d, int flag) {
        for (BodyPart p : BodyPart.values()) {
            set(d, p, flag, false);
        }
    }

    /** Sets a part's flags from its severities. */
    public static void sync(TarkovData d, int i) {
        int c = d.conditions[i] & ~(LIGHT_BLEED | HEAVY_BLEED | FRACTURE);
        if (d.bleed[i] > EPS) {
            c |= d.bleed[i] >= HEAVY ? HEAVY_BLEED : LIGHT_BLEED;
        }
        if (d.fracture[i] > EPS) {
            c |= FRACTURE;
        }
        d.conditions[i] = c;
    }

    /** Saves from before severities: give flagged conditions a middling one. */
    public static void migrate(TarkovData d) {
        for (int i = 0; i < d.conditions.length; i++) {
            int c = d.conditions[i];
            if ((c & HEAVY_BLEED) != 0 && d.bleed[i] <= EPS) {
                d.bleed[i] = 0.7f;
            } else if ((c & LIGHT_BLEED) != 0 && d.bleed[i] <= EPS) {
                d.bleed[i] = 0.3f;
            }
            if ((c & FRACTURE) != 0 && d.fracture[i] <= EPS) {
                d.fracture[i] = 0.5f;
            }
            sync(d, i);
        }
    }

    /** Two severities on the same part stack without going past 100%. */
    public static float combine(float a, float b) {
        return 1 - (1 - Mth.clamp(a, 0, 1)) * (1 - Mth.clamp(b, 0, 1));
    }

    public static void addBleed(TarkovData d, BodyPart p, float severity) {
        d.bleed[p.ordinal()] = combine(d.bleed[p.ordinal()], severity);
        sync(d, p.ordinal());
    }

    public static void addFracture(TarkovData d, BodyPart p, float severity) {
        d.fracture[p.ordinal()] = combine(d.fracture[p.ordinal()], severity);
        sync(d, p.ordinal());
    }

    /** The most a part can heal to: lasting damage takes its share off the top. */
    public static float cap(TarkovData d, int i) {
        return 1 - Mth.clamp(d.scar[i], 0, 0.9f);
    }

    public static void reset(TarkovData d) {
        java.util.Arrays.fill(d.body, 1f);
        java.util.Arrays.fill(d.conditions, 0);
        java.util.Arrays.fill(d.bleed, 0);
        java.util.Arrays.fill(d.fracture, 0);
        java.util.Arrays.fill(d.scar, 0);
        d.painkillerTicks = 0;
    }

    /**
     * Share of max health still reachable: destroyed parts lock theirs away until surgery restores them, and lasting
     * damage locks part of a part's share.
     */
    public static float capFraction(float[] body, float[] scar) {
        float cap = 1;
        for (BodyPart p : BodyPart.values()) {
            int i = p.ordinal();
            cap -= body[i] <= EPS ? p.share : Mth.clamp(scar[i], 0, 0.9f) * p.share;
        }
        return Math.max(0, cap);
    }

    public static float capFraction(TarkovData d) {
        return capFraction(d.body, d.scar);
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
        float took = Math.max(0, Math.min((cap(d, i) - d.body[i]) * max, amount));
        d.body[i] = Mth.clamp(d.body[i] + took / max, 0, cap(d, i));
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
                d.body[i] = Mth.clamp(d.body[i] + want / p.share, 0, Math.max(d.body[i], cap(d, i)));
                left += want - (d.body[i] - before) * p.share;
            }
            delta = left;
        }
    }

    private static boolean eligible(TarkovData d, int i, float delta) {
        return delta < 0 ? d.body[i] > EPS : d.body[i] > EPS && d.body[i] < cap(d, i) - EPS;
    }

    private static void clamp(TarkovData d) {
        for (int i = 0; i < d.body.length; i++) {
            d.body[i] = Mth.clamp(d.body[i], 0, cap(d, i));
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
