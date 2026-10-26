package com.cavmkii.seancraft.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player's character: level and class xp, use-skill xp, allocated web nodes, the five ability slots,
 * points lost to respecs, and current resources. Saved with the player and kept through death.
 */
public final class PlayerData {
    public static final int MAX_LEVEL = 60;
    public static final int SLOTS = 5;
    /** Character level at which each ability slot opens. */
    public static final int[] SLOT_LEVELS = {1, 10, 25, 40, 60};

    public int level = 1;
    public long xp;
    public final Map<Skill, Double> skillXp = new EnumMap<>(Skill.class);
    public final Set<Integer> nodes = new LinkedHashSet<>();
    public final String[] slots = new String[SLOTS];
    public int lostPoints;
    public float mana;
    public float rage;
    public float stamina;

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(d -> d.xp),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("skills", Map.of()).forGetter(PlayerData::skillMap),
            Codec.INT.listOf().optionalFieldOf("nodes", List.of()).forGetter(d -> new ArrayList<>(d.nodes)),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(d -> slotList(d.slots)),
            Codec.INT.optionalFieldOf("lostPoints", 0).forGetter(d -> d.lostPoints),
            Codec.FLOAT.optionalFieldOf("mana", 0f).forGetter(d -> d.mana),
            Codec.FLOAT.optionalFieldOf("stamina", 0f).forGetter(d -> d.stamina)
    ).apply(i, PlayerData::fromSaved));

    private static PlayerData fromSaved(int level, long xp, Map<String, Double> skills, List<Integer> nodes, List<String> slots,
            int lostPoints, float mana, float stamina) {
        PlayerData d = new PlayerData();
        d.level = Math.max(1, Math.min(MAX_LEVEL, level));
        d.xp = xp;
        skills.forEach((name, value) -> {
            try {
                d.skillXp.put(Skill.valueOf(name), value);
            } catch (IllegalArgumentException ignored) {
                // A skill that no longer exists.
            }
        });
        d.nodes.addAll(nodes);
        for (int s = 0; s < SLOTS && s < slots.size(); s++) {
            d.slots[s] = slots.get(s).isEmpty() ? null : slots.get(s);
        }
        d.lostPoints = lostPoints;
        d.mana = mana;
        d.stamina = stamina;
        return d;
    }

    private Map<String, Double> skillMap() {
        Map<String, Double> map = new HashMap<>();
        skillXp.forEach((skill, value) -> map.put(skill.name(), value));
        return map;
    }

    private static List<String> slotList(String[] slots) {
        List<String> list = new ArrayList<>();
        for (String s : slots) {
            list.add(s == null ? "" : s);
        }
        return list;
    }

    // ------------------------------------------------------------------ levels and points

    /** Class xp needed to go from a level to the next. */
    public static long xpToNext(int level) {
        return (long) (40 + 20 * Math.pow(level, 1.4));
    }

    public int skillLevel(Skill skill) {
        return Skill.levelForXp(skillXp.getOrDefault(skill, 0.0));
    }

    public int totalPoints() {
        return level;
    }

    public int spentPoints() {
        return nodes.size();
    }

    public int freePoints() {
        return Math.max(0, totalPoints() - lostPoints - spentPoints());
    }

    public int unlockedSlots() {
        int n = 0;
        for (int need : SLOT_LEVELS) {
            if (level >= need) {
                n++;
            }
        }
        return n;
    }

    public boolean isAllocated(int node) {
        return node == PassiveWeb.START || nodes.contains(node);
    }

    /** A node can be taken if it's free, you have a point, and it links to something you already have. */
    public boolean canAllocate(int id) {
        WebNode node = PassiveWeb.get(id);
        if (node == null || isAllocated(id) || freePoints() <= 0) {
            return false;
        }
        for (int link : node.links) {
            if (isAllocated(link)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActive(ActiveSkill skill) {
        for (int id : nodes) {
            WebNode node = PassiveWeb.get(id);
            if (node != null && node.active == skill) {
                return true;
            }
        }
        return false;
    }

    public ActiveSkill slot(int index) {
        return index >= 0 && index < SLOTS && slots[index] != null ? ActiveSkill.byName(slots[index]) : null;
    }

    // ------------------------------------------------------------------ network

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeVarLong(xp);
        for (Skill skill : Skill.values()) {
            buf.writeDouble(skillXp.getOrDefault(skill, 0.0));
        }
        buf.writeVarInt(nodes.size());
        for (int id : nodes) {
            buf.writeVarInt(id);
        }
        for (String s : slots) {
            buf.writeUtf(s == null ? "" : s);
        }
        buf.writeVarInt(lostPoints);
    }

    public static PlayerData read(FriendlyByteBuf buf) {
        PlayerData d = new PlayerData();
        d.level = buf.readVarInt();
        d.xp = buf.readVarLong();
        for (Skill skill : Skill.values()) {
            d.skillXp.put(skill, buf.readDouble());
        }
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            d.nodes.add(buf.readVarInt());
        }
        for (int s = 0; s < SLOTS; s++) {
            String name = buf.readUtf();
            d.slots[s] = name.isEmpty() ? null : name;
        }
        d.lostPoints = buf.readVarInt();
        return d;
    }
}
