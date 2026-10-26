package com.cavmkii.seancraft.rpg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The passive web: a start node in the middle and six archetype regions around it, 19 nodes each,
 * with bridge nodes linking neighbouring regions. Built the same way on server and client, so node
 * ids match without being sent over the network.
 */
public final class PassiveWeb {
    public static final int START = 0;

    /** Per-region template: radius, angle offset (degrees), kind, and which template nodes it links to. */
    private record Slot(double radius, double angle, WebNode.Kind kind, int[] linksTo) {}

    private static final Slot[] TEMPLATE = {
            /* 0 */ new Slot(1.2, 0, WebNode.Kind.SMALL, new int[] {-1}),
            /* 1 */ new Slot(2.1, 0, WebNode.Kind.SMALL, new int[] {0}),
            /* 2 */ new Slot(2.7, -15, WebNode.Kind.SMALL, new int[] {1}),
            /* 3 */ new Slot(2.7, 15, WebNode.Kind.SMALL, new int[] {1}),
            /* 4 */ new Slot(3.2, 0, WebNode.Kind.ACTIVE, new int[] {1}),
            /* 5 */ new Slot(3.5, -22, WebNode.Kind.NOTABLE, new int[] {2}),
            /* 6 */ new Slot(3.5, 22, WebNode.Kind.NOTABLE, new int[] {3}),
            /* 7 */ new Slot(4.2, 0, WebNode.Kind.SMALL, new int[] {4}),
            /* 8 */ new Slot(4.4, -18, WebNode.Kind.SMALL, new int[] {5}),
            /* 9 */ new Slot(4.4, 18, WebNode.Kind.SMALL, new int[] {6}),
            /* 10 */ new Slot(5.0, -9, WebNode.Kind.ACTIVE, new int[] {7, 8}),
            /* 11 */ new Slot(5.0, 9, WebNode.Kind.ACTIVE, new int[] {7, 9}),
            /* 12 */ new Slot(5.6, -22, WebNode.Kind.SMALL, new int[] {8}),
            /* 13 */ new Slot(5.6, 22, WebNode.Kind.SMALL, new int[] {9}),
            /* 14 */ new Slot(6.0, 0, WebNode.Kind.NOTABLE, new int[] {10, 11}),
            /* 15 */ new Slot(6.6, -15, WebNode.Kind.NOTABLE, new int[] {12}),
            /* 16 */ new Slot(6.6, 15, WebNode.Kind.ACTIVE, new int[] {13}),
            /* 17 */ new Slot(7.0, 0, WebNode.Kind.SMALL, new int[] {14, 15, 16}),
            /* 18 */ new Slot(7.9, 0, WebNode.Kind.KEYSTONE, new int[] {17}),
    };
    private static final int[] ACTIVE_SLOTS = {4, 10, 11, 16};
    private static final int[] NOTABLE_SLOTS = {5, 6, 14, 15};
    private static final int[] MINOR_SLOTS = {7, 17};

    public static final List<WebNode> NODES;

    static {
        List<WebNode> nodes = new ArrayList<>();
        nodes.add(new WebNode(START, WebNode.Kind.START, null, "Start", 0, 0, null));

        int[][] regionIds = new int[6][TEMPLATE.length];
        for (Archetype archetype : Archetype.values()) {
            ActiveSkill[] actives = ActiveSkill.of(archetype);
            int smallIndex = 0;
            for (int t = 0; t < TEMPLATE.length; t++) {
                Slot slot = TEMPLATE[t];
                double angle = archetype.angle() + Math.toRadians(slot.angle);
                double x = Math.sin(angle) * slot.radius;
                double y = -Math.cos(angle) * slot.radius;
                int id = nodes.size();
                regionIds[archetype.ordinal()][t] = id;

                ActiveSkill active = null;
                String name;
                WebNode node;
                if (slot.kind == WebNode.Kind.ACTIVE) {
                    active = actives[indexOf(ACTIVE_SLOTS, t)];
                    node = new WebNode(id, slot.kind, archetype, active.label, x, y, active);
                } else if (slot.kind == WebNode.Kind.NOTABLE) {
                    node = notable(id, archetype, indexOf(NOTABLE_SLOTS, t), x, y);
                } else if (slot.kind == WebNode.Kind.KEYSTONE) {
                    node = keystone(id, archetype, x, y);
                } else if (indexOf(MINOR_SLOTS, t) >= 0) {
                    node = minor(id, archetype, x, y);
                } else {
                    Attr attr = smallIndex++ % 2 == 0 ? archetype.first : archetype.second;
                    name = attr.label;
                    node = new WebNode(id, slot.kind, archetype, name, x, y, null).stat(Stat.valueOf(attr.name()), 3);
                }
                nodes.add(node);
                for (int link : slot.linksTo) {
                    connect(nodes, id, link < 0 ? START : regionIds[archetype.ordinal()][link]);
                }
            }
        }

        // Bridges between neighbouring regions, so hybrids don't have to go back through the middle.
        for (Archetype archetype : Archetype.values()) {
            Archetype next = archetype.next();
            int[] mine = regionIds[archetype.ordinal()];
            int[] theirs = regionIds[next.ordinal()];
            double angle = archetype.angle() + Math.toRadians(30);
            double radius = 4.7;
            int id = nodes.size();
            WebNode bridge = new WebNode(id, WebNode.Kind.SMALL, null, archetype.label + "-" + next.label + " Bridge",
                    Math.sin(angle) * radius, -Math.cos(angle) * radius, null);
            Attr a = archetype.second;
            Attr b = next.first;
            bridge.stat(Stat.valueOf(a.name()), 2).stat(Stat.valueOf(b.name()), 2);
            nodes.add(bridge);
            connect(nodes, id, mine[9]);
            connect(nodes, id, theirs[8]);
            // Inner ring link too.
            connect(nodes, mine[3], theirs[2]);
        }

        NODES = Collections.unmodifiableList(nodes);
    }

    private PassiveWeb() {}

    public static WebNode get(int id) {
        return id >= 0 && id < NODES.size() ? NODES.get(id) : null;
    }

    private static void connect(List<WebNode> nodes, int a, int b) {
        if (!nodes.get(a).links.contains(b)) {
            nodes.get(a).links.add(b);
            nodes.get(b).links.add(a);
        }
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static WebNode minor(int id, Archetype archetype, double x, double y) {
        return switch (archetype) {
            case WARRIOR -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Brawn", x, y, null).stat(Stat.MELEE_DAMAGE, 5).stat(Stat.STR, 2);
            case DUELIST -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Finesse", x, y, null).stat(Stat.ATTACK_SPEED, 4).stat(Stat.AGI, 2);
            case RANGER -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Aim", x, y, null).stat(Stat.PROJECTILE_DAMAGE, 5).stat(Stat.AGI, 2);
            case SHADOW -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Swiftness", x, y, null).stat(Stat.MOVE_SPEED, 4).stat(Stat.AGI, 2);
            case MAGE -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Focus", x, y, null).stat(Stat.SPELL_DAMAGE, 5).stat(Stat.INT, 2);
            case TEMPLAR -> new WebNode(id, WebNode.Kind.SMALL, archetype, "Fortitude", x, y, null).stat(Stat.MAX_HEALTH, 4).stat(Stat.STR, 2);
        };
    }

    private static WebNode notable(int id, Archetype archetype, int index, double x, double y) {
        WebNode.Kind k = WebNode.Kind.NOTABLE;
        return switch (archetype) {
            case WARRIOR -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Iron Skin", x, y, null).stat(Stat.ARMOR, 4).stat(Stat.STR, 4);
                case 1 -> new WebNode(id, k, archetype, "Bloodthirst", x, y, null).stat(Stat.LIFESTEAL, 5);
                case 2 -> new WebNode(id, k, archetype, "Heavy Hitter", x, y, null).stat(Stat.MELEE_DAMAGE, 15);
                default -> new WebNode(id, k, archetype, "Unbreakable", x, y, null).stat(Stat.KNOCKBACK_RES, 30).stat(Stat.HEALTH_REGEN, 0.3);
            };
            case DUELIST -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Precision", x, y, null).stat(Stat.CRIT_CHANCE, 10);
                case 1 -> new WebNode(id, k, archetype, "Footwork", x, y, null).stat(Stat.MOVE_SPEED, 5).stat(Stat.ATTACK_SPEED, 5);
                case 2 -> new WebNode(id, k, archetype, "Momentum", x, y, null).stat(Stat.RAGE_GAIN, 50).stat(Stat.STAMINA_REGEN, 20);
                default -> new WebNode(id, k, archetype, "Duelist's Edge", x, y, null).stat(Stat.MELEE_DAMAGE, 10).stat(Stat.CRIT_CHANCE, 5).stat(Stat.CRIT_DAMAGE, 15);
            };
            case RANGER -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Fleet Foot", x, y, null).stat(Stat.MOVE_SPEED, 8);
                case 1 -> new WebNode(id, k, archetype, "Sharpshooter", x, y, null).stat(Stat.PROJECTILE_DAMAGE, 20);
                case 2 -> new WebNode(id, k, archetype, "Evasion", x, y, null).stat(Stat.DODGE, 10);
                default -> new WebNode(id, k, archetype, "Endurance", x, y, null).stat(Stat.STAMINA, 30).stat(Stat.STAMINA_REGEN, 30);
            };
            case SHADOW -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Assassinate", x, y, null).stat(Stat.BACKSTAB, 40);
                case 1 -> new WebNode(id, k, archetype, "Venom", x, y, null).stat(Stat.VENOM, 3);
                case 2 -> new WebNode(id, k, archetype, "Elusive", x, y, null).stat(Stat.DODGE, 8).stat(Stat.MOVE_SPEED, 4);
                default -> new WebNode(id, k, archetype, "Shadow Arts", x, y, null).stat(Stat.SPELL_DAMAGE, 10).stat(Stat.COOLDOWN, 8);
            };
            case MAGE -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Arcane Mind", x, y, null).stat(Stat.MANA, 25);
                case 1 -> new WebNode(id, k, archetype, "Spell Power", x, y, null).stat(Stat.SPELL_DAMAGE, 20);
                case 2 -> new WebNode(id, k, archetype, "Meditation", x, y, null).stat(Stat.MANA_REGEN, 40);
                default -> new WebNode(id, k, archetype, "Quick Casting", x, y, null).stat(Stat.COOLDOWN, 12);
            };
            case TEMPLAR -> switch (index) {
                case 0 -> new WebNode(id, k, archetype, "Devotion", x, y, null).stat(Stat.HEALTH_REGEN, 0.5);
                case 1 -> new WebNode(id, k, archetype, "Sanctity", x, y, null).stat(Stat.MAX_HEALTH, 15);
                case 2 -> new WebNode(id, k, archetype, "Zeal", x, y, null).stat(Stat.SPELL_DAMAGE, 15).stat(Stat.MELEE_DAMAGE, 5);
                default -> new WebNode(id, k, archetype, "Faith", x, y, null).stat(Stat.MANA, 15).stat(Stat.ARMOR, 3);
            };
        };
    }

    private static WebNode keystone(int id, Archetype archetype, double x, double y) {
        WebNode.Kind k = WebNode.Kind.KEYSTONE;
        return switch (archetype) {
            case WARRIOR -> new WebNode(id, k, archetype, "Juggernaut", x, y, null)
                    .stat(Stat.MAX_HEALTH, 40).stat(Stat.KNOCKBACK_RES, 50).stat(Stat.MOVE_SPEED, -15);
            case DUELIST -> new WebNode(id, k, archetype, "Bladedancer", x, y, null)
                    .stat(Stat.ATTACK_SPEED, 30).stat(Stat.CRIT_CHANCE, 10).stat(Stat.CRIT_DAMAGE, 30).stat(Stat.ARMOR, -6);
            case RANGER -> new WebNode(id, k, archetype, "Wind Walker", x, y, null)
                    .stat(Stat.NO_FALL, 1).stat(Stat.MOVE_SPEED, 15).stat(Stat.MAX_HEALTH, -20);
            case SHADOW -> new WebNode(id, k, archetype, "Ghost", x, y, null)
                    .stat(Stat.GHOST, 1).stat(Stat.BACKSTAB, 30).stat(Stat.MAX_HEALTH, -20);
            case MAGE -> new WebNode(id, k, archetype, "Mind over Matter", x, y, null)
                    .stat(Stat.MIND_OVER_MATTER, 30).stat(Stat.MANA, 20);
            case TEMPLAR -> new WebNode(id, k, archetype, "Holy Aura", x, y, null)
                    .stat(Stat.HOLY_AURA, 1).stat(Stat.HEALTH_REGEN, 0.5).stat(Stat.MOVE_SPEED, -5);
        };
    }
}
