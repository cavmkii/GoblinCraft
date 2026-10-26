package com.cavmkii.goblincraft.tarkov;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Tarkov's hideout modules, built and upgraded at the Hideout Bench. Each hideout keeps its own levels.
 * Valheim's materials are mapped to Minecraft tiers: bronze to copper, iron to iron, nails to nuggets.
 */
public enum Module {
    WORKSHOP("Workshop", Items.SMITHING_TABLE, List.of(
            new Level("Furnaces in the hideout smelt 25% faster. Unlocks the Beta container.", 20,
                    List.of(Cost.planks(32), Cost.of(Items.COBBLESTONE, 16), Cost.of(Items.FLINT, 8))),
            new Level("Furnaces 50% faster. Unlocks the Gamma container.", 40,
                    List.of(Cost.logs(16), Cost.of(Items.COPPER_INGOT, 12), Cost.of(Items.IRON_NUGGET, 16))),
            new Level("Furnaces 75% faster. Unlocks the Kappa container.", 60,
                    List.of(Cost.logs(16), Cost.of(Items.IRON_INGOT, 16), Cost.of(Items.IRON_NUGGET, 32))))),
    MEDSTATION("Medstation", Items.BREWING_STAND, List.of(
            new Level("Unlocks basic meds. Bleeding stops in the hideout.", 20,
                    List.of(Cost.planks(16), Cost.of(Items.LEATHER, 8), Cost.of(Items.STRING, 8))),
            new Level("Unlocks painkillers and field kits. Fractures mend after a minute in the hideout.", 40,
                    List.of(Cost.logs(8), Cost.of(Items.COPPER_INGOT, 6), Cost.of(Items.HONEY_BOTTLE, 6)),
                    WORKSHOP, 1),
            new Level("Unlocks surgical kits. Faster health regeneration in the hideout.", 60,
                    List.of(Cost.of(Items.IRON_INGOT, 10), Cost.of(Items.SLIME_BALL, 8), Cost.of(Items.GLISTERING_MELON_SLICE, 8)),
                    WORKSHOP, 2))),
    PERIMETER("Perimeter", Items.STONE_BRICK_WALL, List.of(
            new Level("Hideout radius +10 blocks.", 20,
                    List.of(Cost.planks(32), Cost.of(Items.COBBLESTONE, 32))),
            new Level("Hideout radius +20 blocks.", 40,
                    List.of(Cost.of(Items.STONE_BRICKS, 32), Cost.of(Items.COPPER_INGOT, 8)), WORKSHOP, 1),
            new Level("Hideout radius +30 blocks.", 60,
                    List.of(Cost.of(Items.STONE_BRICKS, 64), Cost.of(Items.IRON_INGOT, 16)), WORKSHOP, 2))),
    INTEL("Intel center", Items.LECTERN, List.of(
            new Level("Raids start with 3 more calm minutes.", 20,
                    List.of(Cost.planks(16), Cost.of(Items.FEATHER, 8), Cost.of(Items.REDSTONE, 8)), PERIMETER, 1),
            new Level("Raids start with 6 more calm minutes, and you're warned when a hunting party is sent.", 40,
                    List.of(Cost.of(Items.COPPER_INGOT, 8), Cost.of(Items.REDSTONE, 16), Cost.of(Items.COMPASS, 1)), WORKSHOP, 2)));

    /** One level of a module: what it does, how long it takes, what it costs, and what it needs first. */
    public record Level(String effect, int buildSeconds, List<Cost> costs, Module needs, int needsLevel) {
        Level(String effect, int buildSeconds, List<Cost> costs) {
            this(effect, buildSeconds, costs, null, 0);
        }
    }

    public static final double PERIMETER_STEP = 10;
    public static final double INTEL_CALM_MINUTES = 3;

    public final String label;
    public final Item icon;
    public final List<Level> levels;

    Module(String label, Item icon, List<Level> levels) {
        this.label = label;
        this.icon = icon;
        this.levels = levels;
    }

    public int max() {
        return levels.size();
    }
}
