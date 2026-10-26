package com.cavmkii.goblincraft.tarkov;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Per-player Tarkov state: the secure container and whether the starter kit was given. Kept through death. */
public final class TarkovData {
    public SecureTier tier = SecureTier.ALPHA;
    public final NonNullList<ItemStack> secure = NonNullList.withSize(SecureTier.KAPPA.slots(), ItemStack.EMPTY);
    public boolean starterGiven;
    /** Intel level of the last hideout you were in (home isn't loaded while you're out). */
    public int lastIntel;

    public static final Codec<TarkovData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("tier", "ALPHA").forGetter(d -> d.tier.name()),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("secure", List.of()).forGetter(d -> new ArrayList<>(d.secure)),
            Codec.BOOL.optionalFieldOf("starterGiven", false).forGetter(d -> d.starterGiven),
            Codec.INT.optionalFieldOf("lastIntel", 0).forGetter(d -> d.lastIntel)
    ).apply(i, (tier, secure, starter, intel) -> {
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
        return d;
    }));
}
