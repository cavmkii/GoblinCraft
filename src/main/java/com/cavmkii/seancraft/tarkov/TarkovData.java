package com.cavmkii.seancraft.tarkov;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player Tarkov state: secure pouch tier, whether the starter kit was given, body health. Kept through death.
 * {@code secure} is the old slot-based container, emptied into the pouch grid on first load.
 */
public final class TarkovData {
    public SecureTier tier = SecureTier.ALPHA;
    public final NonNullList<ItemStack> secure = NonNullList.withSize(SecureTier.KAPPA.slots(), ItemStack.EMPTY);
    public boolean starterGiven;
    /** Intel level of the last hideout you were in (home isn't loaded while you're out). */
    public int lastIntel;
    /** Health of each body part as a fraction of its share (see {@link BodyPart}). */
    public final float[] body = {1, 1, 1, 1, 1, 1, 1};
    /** Conditions per body part, as {@link Body} bit flags. */
    public final int[] conditions = new int[7];
    public int painkillerTicks;
    /** Bleed severity per part, 0-1. At 0.5 and up it's a heavy bleed. Kept in step with {@link #conditions}. */
    public final float[] bleed = new float[7];
    /** Fracture severity per part, 0-1. */
    public final float[] fracture = new float[7];
    /**
     * Lasting damage per part, 0-1: the share of the part's health it can't get back. Left by a surgical kit; a
     * surgery case clears it.
     */
    public final float[] scar = new float[7];
    /** Whether a new player has been placed at a spawn point yet. */
    public boolean placedAtSpawn;

    public static final Codec<TarkovData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("tier", "ALPHA").forGetter(d -> d.tier.name()),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("secure", List.of()).forGetter(d -> new ArrayList<>(d.secure)),
            Codec.BOOL.optionalFieldOf("starterGiven", false).forGetter(d -> d.starterGiven),
            Codec.INT.optionalFieldOf("lastIntel", 0).forGetter(d -> d.lastIntel),
            Codec.FLOAT.listOf().optionalFieldOf("body", List.of()).forGetter(d -> {
                List<Float> list = new ArrayList<>();
                for (float f : d.body) {
                    list.add(f);
                }
                return list;
            }),
            Codec.INT.listOf().optionalFieldOf("conditions", List.of()).forGetter(d -> {
                List<Integer> list = new ArrayList<>();
                for (int c : d.conditions) {
                    list.add(c);
                }
                return list;
            }),
            Codec.INT.optionalFieldOf("painkiller", 0).forGetter(d -> d.painkillerTicks),
            Codec.BOOL.optionalFieldOf("placedAtSpawn", false).forGetter(d -> d.placedAtSpawn),
            Codec.FLOAT.listOf().optionalFieldOf("bleed", List.of()).forGetter(d -> floats(d.bleed)),
            Codec.FLOAT.listOf().optionalFieldOf("fracture", List.of()).forGetter(d -> floats(d.fracture)),
            Codec.FLOAT.listOf().optionalFieldOf("scar", List.of()).forGetter(d -> floats(d.scar))
    ).apply(i, (tier, secure, starter, intel, body, conditions, painkiller, placed, bleed, fracture, scar) -> {
        TarkovData d = new TarkovData();
        try {
            d.tier = SecureTier.valueOf(tier);
        } catch (IllegalArgumentException ignored) {
            d.tier = SecureTier.ALPHA;
        }
        for (int s = 0; s < secure.size() && s < d.secure.size(); s++) {
            d.secure.set(s, secure.get(s));
        }
        d.starterGiven = starter;
        d.lastIntel = intel;
        for (int p = 0; p < body.size() && p < d.body.length; p++) {
            d.body[p] = body.get(p);
        }
        for (int p = 0; p < conditions.size() && p < d.conditions.length; p++) {
            d.conditions[p] = conditions.get(p);
        }
        d.painkillerTicks = painkiller;
        d.placedAtSpawn = placed;
        fill(d.bleed, bleed);
        fill(d.fracture, fracture);
        fill(d.scar, scar);
        Body.migrate(d);
        return d;
    }));

    private static List<Float> floats(float[] a) {
        List<Float> list = new ArrayList<>();
        for (float f : a) {
            list.add(f);
        }
        return list;
    }

    private static void fill(float[] into, List<Float> from) {
        for (int p = 0; p < from.size() && p < into.length; p++) {
            into[p] = from.get(p);
        }
    }
}
