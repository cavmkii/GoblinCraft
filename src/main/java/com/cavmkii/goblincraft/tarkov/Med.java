package com.cavmkii.goblincraft.tarkov;

import java.util.List;
import net.minecraft.world.item.Items;

/** Tarkov meds, made at the Hideout Bench once its Medstation is built. */
public enum Med {
    BANDAGE("bandage", "Bandage", 40, 10, true, false, false, 0, 0, false, false, Module.MEDSTATION, 1,
            List.of(Cost.of(Items.STRING, 2), Cost.of(Items.DANDELION, 1))),
    TOURNIQUET("tourniquet", "Tourniquet", 40, 5, true, true, false, 0, 0, true, false, Module.MEDSTATION, 1,
            List.of(Cost.of(Items.LEATHER, 1), Cost.of(Items.STRING, 2), Cost.of(Items.SLIME_BALL, 1))),
    SPLINT("splint", "Splint", 60, 5, false, false, true, 0, 0, true, false, Module.MEDSTATION, 1,
            List.of(Cost.of(Items.STICK, 3), Cost.of(Items.STRING, 2))),
    PAINKILLERS("painkillers", "Painkillers", 24, 10, false, false, false, 20 * 60 * 5, 0, false, false, Module.MEDSTATION, 2,
            List.of(Cost.of(Items.POPPY, 2), Cost.of(Items.SUGAR, 1))),
    FIELD_KIT("field_kit", "Field kit", 60, 3, true, true, false, 0, 0.3f, false, false, Module.MEDSTATION, 2,
            List.of(Cost.of(Items.HONEY_BOTTLE, 2), Cost.of(Items.STRING, 3), Cost.of(Items.SLIME_BALL, 2))),
    SURGICAL_KIT("surgical_kit", "Surgical kit", 100, 2, true, true, true, 0, 0.5f, false, true, Module.MEDSTATION, 3,
            List.of(Cost.of(Items.IRON_INGOT, 2), Cost.of(Items.STRING, 4), Cost.of(Items.HONEY_BOTTLE, 3), Cost.of(Items.POPPY, 2)));

    public final String id;
    public final String label;
    public final int useTicks;
    public final int stack;
    public final boolean stopsLight;
    public final boolean stopsHeavy;
    public final boolean fixesFracture;
    public final int painkillerTicks;
    /** Fraction of max health put into the treated part. */
    public final float heal;
    public final boolean limbsOnly;
    public final boolean revives;
    public final Module gate;
    public final int gateLevel;
    public final List<Cost> costs;

    Med(String id, String label, int useTicks, int stack, boolean stopsLight, boolean stopsHeavy, boolean fixesFracture,
            int painkillerTicks, float heal, boolean limbsOnly, boolean revives, Module gate, int gateLevel, List<Cost> costs) {
        this.id = id;
        this.label = label;
        this.useTicks = useTicks;
        this.stack = stack;
        this.stopsLight = stopsLight;
        this.stopsHeavy = stopsHeavy;
        this.fixesFracture = fixesFracture;
        this.painkillerTicks = painkillerTicks;
        this.heal = heal;
        this.limbsOnly = limbsOnly;
        this.revives = revives;
        this.gate = gate;
        this.gateLevel = gateLevel;
        this.costs = costs;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (stopsHeavy) {
            sb.append("Stops any bleed. ");
        } else if (stopsLight) {
            sb.append("Stops a light bleed. ");
        }
        if (fixesFracture) {
            sb.append("Sets a fracture. ");
        }
        if (revives) {
            sb.append("Restores a destroyed part. ");
        }
        if (heal > 0) {
            sb.append("Heals the part ").append((int) (heal * 100)).append("% of max health. ");
        }
        if (painkillerTicks > 0) {
            sb.append("Hides fracture and destroyed-limb effects for 5 minutes. ");
        }
        if (limbsOnly) {
            sb.append("Arms and legs only. ");
        }
        return sb.toString().trim();
    }
}
