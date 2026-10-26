package com.cavmkii.goblincraft.rpg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** One node of the passive web. Positions are in web units; the screen scales them. */
public final class WebNode {
    public enum Kind { START, SMALL, NOTABLE, ACTIVE, KEYSTONE }

    public final int id;
    public final Kind kind;
    public final Archetype archetype;
    public final String name;
    public final double x;
    public final double y;
    public final Map<Stat, Double> stats = new EnumMap<>(Stat.class);
    public final ActiveSkill active;
    public final List<Integer> links = new ArrayList<>();

    WebNode(int id, Kind kind, Archetype archetype, String name, double x, double y, ActiveSkill active) {
        this.id = id;
        this.kind = kind;
        this.archetype = archetype;
        this.name = name;
        this.x = x;
        this.y = y;
        this.active = active;
    }

    WebNode stat(Stat stat, double value) {
        stats.merge(stat, value, Double::sum);
        return this;
    }

    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (active != null) {
            lines.add("Active: " + active.description);
            lines.add("Costs " + active.cost + " " + active.resource.label + ", " + active.cooldownTicks / 20.0 + "s cooldown");
        }
        stats.forEach((stat, value) -> lines.add(stat.describe(value)));
        return lines;
    }
}
