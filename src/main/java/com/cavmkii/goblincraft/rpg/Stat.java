package com.cavmkii.goblincraft.rpg;

/**
 * Everything a passive node can grant. Percent stats are in whole percent; flags are 1 when present.
 */
public enum Stat {
    STR("%+.0f Strength"),
    AGI("%+.0f Agility"),
    INT("%+.0f Intelligence"),
    MAX_HEALTH("%+.0f%% maximum health"),
    ARMOR("%+.0f armor"),
    MELEE_DAMAGE("%+.0f%% melee damage"),
    PROJECTILE_DAMAGE("%+.0f%% projectile damage"),
    SPELL_DAMAGE("%+.0f%% ability damage"),
    ATTACK_SPEED("%+.0f%% attack speed"),
    MOVE_SPEED("%+.0f%% movement speed"),
    LIFESTEAL("%.0f%% of melee damage dealt heals you"),
    DODGE("%.0f%% chance to dodge attacks"),
    CRIT_CHANCE("%.0f%% chance for melee hits to deal 50%% more"),
    MANA("%+.0f%% maximum mana"),
    MANA_REGEN("%+.0f%% mana regeneration"),
    RAGE_GAIN("%+.0f%% rage gained"),
    STAMINA("%+.0f%% maximum stamina"),
    STAMINA_REGEN("%+.0f%% stamina regeneration"),
    COOLDOWN("%.0f%% faster cooldowns"),
    KNOCKBACK_RES("%+.0f%% knockback resistance"),
    HEALTH_REGEN("%+.1f health regenerated per second"),
    BACKSTAB("%+.0f%% melee damage when hitting from behind"),
    VENOM("Melee hits poison for %.0f seconds"),
    NO_FALL("No fall damage"),
    MIND_OVER_MATTER("%.0f%% of damage taken comes from mana first"),
    GHOST("Sneaking makes you invisible"),
    HOLY_AURA("Burn nearby monsters every second");

    private final String format;

    Stat(String format) {
        this.format = format;
    }

    public String describe(double value) {
        // Flag formats have no placeholder; the extra argument is ignored.
        return String.format(format, value);
    }
}
