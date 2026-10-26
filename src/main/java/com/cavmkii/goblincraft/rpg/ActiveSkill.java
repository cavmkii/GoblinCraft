package com.cavmkii.goblincraft.rpg;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Active skills. Each comes from one node on the web and costs its archetype's resource. */
public enum ActiveSkill {
    // Warrior: rage
    LEAP_SLAM(Archetype.WARRIOR, "Leap Slam", "Leap forward and smash the ground where you land.", Resource.RAGE, 25, 8, Items.IRON_BOOTS),
    WAR_CRY(Archetype.WARRIOR, "War Cry", "Gain Strength and absorption; nearby enemies are weakened and slowed.", Resource.RAGE, 30, 20, Items.GOAT_HORN),
    GROUND_SLAM(Archetype.WARRIOR, "Ground Slam", "Smash the ground, damaging and launching enemies around you.", Resource.RAGE, 20, 6, Items.MACE),
    BERSERK(Archetype.WARRIOR, "Berserk", "Strength II, Speed and Haste for 12 seconds.", Resource.RAGE, 40, 40, Items.NETHERITE_AXE),
    // Duelist: stamina and rage
    LUNGE(Archetype.DUELIST, "Lunge", "Dash to the enemy you're looking at and strike hard.", Resource.STAMINA, 20, 5, Items.IRON_SWORD),
    RIPOSTE(Archetype.DUELIST, "Riposte", "For 2 seconds, the next hit is parried and countered for double damage.", Resource.RAGE, 15, 6, Items.SHIELD),
    WHIRLWIND(Archetype.DUELIST, "Whirlwind", "Spin three times, hitting everything around you.", Resource.RAGE, 30, 10, Items.DIAMOND_SWORD),
    BLADE_FLURRY(Archetype.DUELIST, "Blade Flurry", "Five rapid strikes on the enemy in front of you.", Resource.STAMINA, 35, 12, Items.GOLDEN_SWORD),
    // Ranger: stamina
    DASH(Archetype.RANGER, "Dash", "Dash forward, briefly untouchable.", Resource.STAMINA, 20, 3, Items.FEATHER),
    VOLLEY(Archetype.RANGER, "Volley", "Fire five arrows in a spread.", Resource.STAMINA, 30, 8, Items.BOW),
    RAIN_OF_ARROWS(Archetype.RANGER, "Rain of Arrows", "Arrows rain down where you're looking.", Resource.STAMINA, 40, 14, Items.ARROW),
    HUNTERS_MARK(Archetype.RANGER, "Hunter's Mark", "Mark an enemy: it glows and takes 30% more damage for 10 seconds.", Resource.STAMINA, 15, 12, Items.SPECTRAL_ARROW),
    // Shadow: stamina and mana
    SMOKE_BOMB(Archetype.SHADOW, "Smoke Bomb", "Turn invisible; nearby enemies are blinded and lose track of you.", Resource.STAMINA, 25, 16, Items.GUNPOWDER),
    SHADOW_STEP(Archetype.SHADOW, "Shadow Step", "Teleport behind the enemy you're looking at.", Resource.MANA, 20, 6, Items.ENDER_PEARL),
    POISON_CLOUD(Archetype.SHADOW, "Poison Cloud", "Leave a cloud of poison where you're looking.", Resource.MANA, 30, 12, Items.SPIDER_EYE),
    BLADE_FAN(Archetype.SHADOW, "Blade Fan", "Throw eight daggers in a ring around you.", Resource.STAMINA, 30, 9, Items.IRON_NUGGET),
    // Mage: mana
    FIREBALL(Archetype.MAGE, "Fireball", "Hurl a fireball that bursts on impact. Never breaks blocks.", Resource.MANA, 20, 1.5, Items.FIRE_CHARGE),
    FROST_NOVA(Archetype.MAGE, "Frost Nova", "Freeze and damage everything around you.", Resource.MANA, 30, 10, Items.BLUE_ICE),
    BLINK(Archetype.MAGE, "Blink", "Teleport up to 12 blocks where you're looking.", Resource.MANA, 25, 6, Items.CHORUS_FRUIT),
    CHAIN_LIGHTNING(Archetype.MAGE, "Chain Lightning", "Lightning that leaps between up to five enemies.", Resource.MANA, 40, 8, Items.LIGHTNING_ROD),
    // Templar: mana and rage
    CONSECRATE(Archetype.TEMPLAR, "Consecrate", "Hallow the ground for 8 seconds: heals players, burns monsters.", Resource.MANA, 30, 16, Items.GLOWSTONE_DUST),
    HOLY_SMITE(Archetype.TEMPLAR, "Holy Smite", "Call a bolt of holy lightning where you're looking.", Resource.MANA, 25, 7, Items.BLAZE_ROD),
    DIVINE_SHIELD(Archetype.TEMPLAR, "Divine Shield", "Six hearts of absorption and Resistance for 10 seconds.", Resource.RAGE, 35, 30, Items.TOTEM_OF_UNDYING),
    JUDGMENT(Archetype.TEMPLAR, "Judgment", "Stagger everything around you: damage, slowness and weakness.", Resource.RAGE, 25, 10, Items.ANVIL);

    public final Archetype archetype;
    public final String label;
    public final String description;
    public final Resource resource;
    public final int cost;
    public final int cooldownTicks;
    public final Item icon;

    ActiveSkill(Archetype archetype, String label, String description, Resource resource, int cost, double cooldownSeconds, Item icon) {
        this.archetype = archetype;
        this.label = label;
        this.description = description;
        this.resource = resource;
        this.cost = cost;
        this.cooldownTicks = (int) Math.round(cooldownSeconds * 20);
        this.icon = icon;
    }

    public static ActiveSkill byName(String name) {
        for (ActiveSkill skill : values()) {
            if (skill.name().equals(name)) {
                return skill;
            }
        }
        return null;
    }

    /** The four skills of an archetype, in web order (A, B, C, D). */
    public static ActiveSkill[] of(Archetype archetype) {
        ActiveSkill[] out = new ActiveSkill[4];
        int i = 0;
        for (ActiveSkill skill : values()) {
            if (skill.archetype == archetype) {
                out[i++] = skill;
            }
        }
        return out;
    }
}
