package com.cavmkii.goblincraft.rpg;

import java.util.EnumMap;
import java.util.Map;

/**
 * Everything derived from a character: attributes (from use-skills, level and web nodes), the primary
 * attribute, summed node stats, and resource maximums. Pure arithmetic, so the client shows the same
 * numbers the server uses.
 */
public final class Stats {
    public final Map<Stat, Double> node = new EnumMap<>(Stat.class);
    public final Map<Attr, Double> attrs = new EnumMap<>(Attr.class);
    public final Attr primary;

    public Stats(PlayerData data) {
        for (int id : data.nodes) {
            WebNode n = PassiveWeb.get(id);
            if (n != null) {
                n.stats.forEach((stat, value) -> node.merge(stat, value, Double::sum));
            }
        }
        for (Attr attr : Attr.values()) {
            double fromSkills = 0;
            for (Skill skill : Skill.values()) {
                if (skill.attr == attr) {
                    fromSkills += data.skillLevel(skill);
                }
            }
            double value = fromSkills / 10.0 + data.level * 0.25 + get(Stat.valueOf(attr.name()));
            attrs.put(attr, value);
        }
        Attr best = Attr.STR;
        for (Attr attr : Attr.values()) {
            if (attrs.get(attr) > attrs.get(best)) {
                best = attr;
            }
        }
        primary = best;
    }

    public double get(Stat stat) {
        return node.getOrDefault(stat, 0.0);
    }

    public boolean has(Stat stat) {
        return get(stat) > 0;
    }

    public double attr(Attr attr) {
        return attrs.get(attr);
    }

    public double str() { return attr(Attr.STR); }
    public double agi() { return attr(Attr.AGI); }
    public double intel() { return attr(Attr.INT); }

    // Dota-style attribute effects.
    public double bonusHealth() { return str() * 0.4; }
    public double healthRegen() { return str() * 0.01 + get(Stat.HEALTH_REGEN); }
    public double attackSpeedPct() { return agi() * 0.6 + get(Stat.ATTACK_SPEED); }
    public double moveSpeedPct() { return agi() * 0.25 + get(Stat.MOVE_SPEED); }
    public double bonusArmor() { return agi() * 0.08 + get(Stat.ARMOR); }
    public double primaryDamage() { return attr(primary) * 0.04; }
    public double spellPowerPct() { return intel() * 1.0 + get(Stat.SPELL_DAMAGE); }

    public float maxMana() { return (float) ((50 + intel() * 3) * (1 + get(Stat.MANA) / 100)); }
    public float manaRegen() { return (float) ((1 + intel() * 0.05) * (1 + get(Stat.MANA_REGEN) / 100)); }
    public float maxRage() { return (float) (50 + str()); }
    public float maxStamina() { return (float) ((50 + agi() * 2) * (1 + get(Stat.STAMINA) / 100)); }
    public float staminaRegen() { return (float) ((5 + agi() * 0.1) * (1 + get(Stat.STAMINA_REGEN) / 100)); }
    public double cooldownMultiplier(int castingLevel) {
        return Math.max(0.4, 1 - get(Stat.COOLDOWN) / 100 - castingLevel * 0.002);
    }
}
