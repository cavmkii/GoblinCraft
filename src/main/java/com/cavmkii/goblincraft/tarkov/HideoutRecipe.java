package com.cavmkii.goblincraft.tarkov;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Things only the Hideout Bench makes, some gated behind module levels. Later phases add meds, rigs and packs. */
public record HideoutRecipe(String id, Supplier<ItemStack> result, List<Cost> costs, Module gate, int gateLevel) {
    public static final List<HideoutRecipe> ALL = new ArrayList<>();

    static {
        ALL.add(new HideoutRecipe("secure_beta", () -> new ItemStack(TarkovContent.SECURE_BETA),
                List.of(Cost.of(Items.COPPER_INGOT, 16), Cost.of(Items.IRON_INGOT, 4)), Module.WORKSHOP, 1));
        ALL.add(new HideoutRecipe("secure_gamma", () -> new ItemStack(TarkovContent.SECURE_GAMMA),
                List.of(Cost.of(Items.IRON_INGOT, 16), Cost.of(Items.GOLD_INGOT, 10)), Module.WORKSHOP, 2));
        ALL.add(new HideoutRecipe("secure_kappa", () -> new ItemStack(TarkovContent.SECURE_KAPPA),
                List.of(Cost.of(Items.DIAMOND, 8), Cost.of(Items.GOLD_INGOT, 20)), Module.WORKSHOP, 3));
    }

    public static HideoutRecipe byId(String id) {
        for (HideoutRecipe r : ALL) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }
}
