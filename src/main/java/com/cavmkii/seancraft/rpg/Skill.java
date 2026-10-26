package com.cavmkii.seancraft.rpg;

/**
 * Valheim-style use-skills. Each ranks 0-100 from use, improves its own action, and feeds one attribute.
 * The xp multiplier evens out how often each one gets exercised.
 */
public enum Skill {
    SWORDS("Swords", Attr.STR, "+%d%% damage with swords"),
    AXES("Axes", Attr.STR, "+%d%% damage with axes"),
    BLOCKING("Blocking", Attr.STR, "Reflect %d%% of blocked damage"),
    MINING("Mining", Attr.STR, "+%d%% block breaking speed"),
    BOWS("Bows & Crossbows", Attr.AGI, "+%d%% arrow damage"),
    RUNNING("Running", Attr.AGI, "+%d%% sprint speed"),
    SNEAKING("Sneaking", Attr.AGI, "+%d%% sneaking speed"),
    SWIMMING("Swimming", Attr.AGI, "+%d%% swim speed, longer breath"),
    ENCHANTING("Enchanting", Attr.INT, "%d%% chance to refund enchanting levels"),
    BREWING("Brewing", Attr.INT, "Potions you drink last +%d%% longer"),
    CASTING("Casting", Attr.INT, "+%d%% ability damage, faster cooldowns"),
    FARMING("Farming", Attr.INT, "%d%% chance of extra crops");

    public static final int MAX_LEVEL = 100;

    public final String label;
    public final Attr attr;
    private final String effectFormat;

    Skill(String label, Attr attr, String effectFormat) {
        this.label = label;
        this.attr = attr;
        this.effectFormat = effectFormat;
    }

    /** Total xp needed to reach a level. */
    public static double xpForLevel(int level) {
        return 5 * Math.pow(level, 1.7);
    }

    public static int levelForXp(double xp) {
        int level = 0;
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /** The skill's own bonus at a level, as the number shown in its description. */
    public int effectValue(int level) {
        return switch (this) {
            case SWORDS, AXES, BOWS -> level / 2;
            case BLOCKING -> level / 2;
            case MINING -> level;
            case RUNNING -> (int) (level * 0.15);
            case SNEAKING -> level;
            case SWIMMING -> level / 2;
            case ENCHANTING -> level * 4 / 10;
            case BREWING -> level / 2;
            case CASTING -> level / 4;
            case FARMING -> level / 2;
        };
    }

    public String describe(int level) {
        return String.format(effectFormat, effectValue(level));
    }
}
