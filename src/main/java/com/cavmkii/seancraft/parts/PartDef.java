package com.cavmkii.seancraft.parts;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * One part: a kind at a tier (1-5). Stat formulas follow TarkoValheim: each tier adds a step, and levels 2-4
 * add a smaller step, so a maxed part slightly beats the next material at level 1.
 */
public record PartDef(String id, PartKind kind, int tier) {
    public static final int MAX_LEVEL = 4;
    public static final String[] METALS = {"copper", "iron", "gold", "diamond", "netherite"};
    public static final String[] METAL_LABELS = {"Copper", "Iron", "Gold", "Diamond", "Netherite"};
    public static final String[] WOODS = {"planks", "log", "bone", "blaze", "breeze"};
    public static final String[] WOOD_LABELS = {"Plank", "Log", "Bone", "Blaze rod", "Breeze rod"};
    public static final String[] HIDES = {"string", "leather", "rabbit", "membrane", "scute"};
    public static final String[] HIDE_LABELS = {"String", "Leather", "Rabbit hide", "Membrane", "Scute"};

    public static final List<PartDef> ALL = new ArrayList<>();

    static {
        for (int t = 1; t <= 5; t++) {
            for (PartKind kind : PartKind.values()) {
                String material = switch (kind.material) {
                    case METAL -> METALS[t - 1];
                    case WOOD -> WOODS[t - 1];
                    case HIDE -> HIDES[t - 1];
                };
                ALL.add(new PartDef(kind.name().toLowerCase() + "_" + material, kind, t));
            }
        }
    }

    public static PartDef byId(String id) {
        for (PartDef def : ALL) {
            if (def.id.equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static PartDef of(PartKind kind, int tier) {
        for (PartDef def : ALL) {
            if (def.kind == kind && def.tier == tier) {
                return def;
            }
        }
        return null;
    }

    public String materialLabel() {
        return switch (kind.material) {
            case METAL -> METAL_LABELS[tier - 1];
            case WOOD -> WOOD_LABELS[tier - 1];
            case HIDE -> HIDE_LABELS[tier - 1];
        };
    }

    public String label() {
        return materialLabel() + " " + kind.label.toLowerCase();
    }

    /** What a level-up costs: the part's own material. */
    public Item material() {
        return switch (kind.material) {
            case METAL -> new Item[] {Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_SCRAP}[tier - 1];
            case WOOD -> new Item[] {Items.OAK_PLANKS, Items.OAK_LOG, Items.BONE, Items.BLAZE_ROD, Items.BREEZE_ROD}[tier - 1];
            case HIDE -> new Item[] {Items.STRING, Items.LEATHER, Items.RABBIT_HIDE, Items.PHANTOM_MEMBRANE, Items.ARMADILLO_SCUTE}[tier - 1];
        };
    }

    /** Materials to go from {@code level} to {@code level + 1}. */
    public int upgradeCost(int level) {
        return (kind.material == PartKind.Material.METAL ? 1 : 2) * (level + 1);
    }

    private static float at(float base, float step, int level) {
        return base + step * (Math.max(1, Math.min(MAX_LEVEL, level)) - 1);
    }

    // Stats (fractions), by kind.
    public float damage(int level) {
        return kind.slot == PartSlot.HEAD ? at(0.06f * tier, 0.025f, level) : 0;
    }

    public float durability(int level) {
        return switch (kind) {
            case BLADE, AXE_HEAD, LIMBS, PLATE -> at(0.05f * tier, 0.05f, level);
            case HANDLE, PADDING -> at(0.04f * tier, 0.04f, level);
            default -> 0;
        };
    }

    /** Negative = cheaper attacks, which in Minecraft means faster swings. */
    public float stamina(int level) {
        return switch (kind) {
            case HANDLE -> at(-0.03f * tier, -0.015f, level);
            case WRAP -> at(-0.02f * tier, -0.01f, level);
            default -> 0;
        };
    }

    public float block(int level) {
        return switch (kind) {
            case GUARD -> at(0.06f * tier, 0.025f, level);
            case WRAP -> at(0.02f * tier, 0.01f, level);
            default -> 0;
        };
    }

    public float parry(int level) {
        return kind == PartKind.GUARD ? at(0.05f * tier, 0.02f, level) : 0;
    }

    /** Gold heads (Valheim's silver) hurt undead more. */
    public float spirit(int level) {
        return kind.slot == PartSlot.HEAD && tier == 3 ? at(0.10f, 0.03f, level) : 0;
    }

    /** Netherite heads (Valheim's flametal) set things alight. */
    public float fire(int level) {
        return kind.slot == PartSlot.HEAD && tier == 5 ? at(0.10f, 0.03f, level) : 0;
    }

    public float armor(int level) {
        return switch (kind) {
            case PLATE -> at(0.06f * tier, 0.025f, level);
            case PADDING -> at(0.02f * tier, 0.01f, level);
            default -> 0;
        };
    }

    /** Negative = lighter armor, which here means a little faster. */
    public float weight(int level) {
        return kind == PartKind.WRAP ? at(-0.04f * tier, -0.02f, level) : 0;
    }
}
