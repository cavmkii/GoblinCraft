package com.cavmkii.seancraft.tarkov;

import com.cavmkii.seancraft.Cfg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Every Hideout Bench in one dimension, with its module levels. Saved with that dimension. */
public final class Hideouts extends SavedData {
    private static final String NAME = "seancraft_hideouts";

    public static final class Hideout {
        public final BlockPos pos;
        public final int[] levels = new int[Module.values().length];
        /** Module being built, or -1. */
        public int building = -1;
        public long buildDone;
        public long buildStarted;
        /** Materials put toward each module's next level so far, per cost line. */
        public final int[][] given = new int[Module.values().length][];

        Hideout(BlockPos pos) {
            this.pos = pos;
        }

        public int level(Module module) {
            return levels[module.ordinal()];
        }

        /** Contributions toward a module's next level, sized to its cost list. */
        public int[] given(Module module) {
            int i = module.ordinal();
            int lines = level(module) < module.max() ? module.levels.get(level(module)).costs().size() : 0;
            if (given[i] == null || given[i].length != lines) {
                int[] fresh = new int[lines];
                if (given[i] != null) {
                    System.arraycopy(given[i], 0, fresh, 0, Math.min(lines, given[i].length));
                }
                given[i] = fresh;
            }
            return given[i];
        }

        public double radius() {
            return Cfg.hideoutRadius + level(Module.PERIMETER) * Module.PERIMETER_STEP;
        }

        public boolean contains(Vec3 point) {
            double r = radius();
            return Vec3.atCenterOf(pos).distanceToSqr(point) <= r * r;
        }
    }

    public final List<Hideout> list = new ArrayList<>();

    public static Hideouts get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Hideouts::new, Hideouts::load, null), NAME);
    }

    /** The hideout whose zone contains a point, or null (the biggest-level one if zones overlap). */
    public static Hideout at(ServerLevel level, Vec3 point) {
        Hideout best = null;
        for (Hideout h : get(level).list) {
            if (h.contains(point) && (best == null || h.radius() > best.radius())) {
                best = h;
            }
        }
        return best;
    }

    public static Hideout byPos(ServerLevel level, BlockPos pos) {
        for (Hideout h : get(level).list) {
            if (h.pos.equals(pos)) {
                return h;
            }
        }
        return null;
    }

    void add(BlockPos pos) {
        if (byPosIn(pos) == null) {
            list.add(new Hideout(pos.immutable()));
            setDirty();
        }
    }

    void remove(BlockPos pos) {
        if (list.removeIf(h -> h.pos.equals(pos))) {
            setDirty();
        }
    }

    private Hideout byPosIn(BlockPos pos) {
        for (Hideout h : list) {
            if (h.pos.equals(pos)) {
                return h;
            }
        }
        return null;
    }

    private static Hideouts load(CompoundTag tag, HolderLookup.Provider registries) {
        Hideouts data = new Hideouts();
        for (Tag t : tag.getList("hideouts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Hideout h = new Hideout(BlockPos.of(c.getLong("pos")));
            int[] levels = c.getIntArray("levels");
            System.arraycopy(levels, 0, h.levels, 0, Math.min(levels.length, h.levels.length));
            h.building = c.contains("building") ? c.getInt("building") : -1;
            h.buildDone = c.getLong("buildDone");
            h.buildStarted = c.getLong("buildStarted");
            for (Module m : Module.values()) {
                if (c.contains("given_" + m.name())) {
                    h.given[m.ordinal()] = c.getIntArray("given_" + m.name());
                }
            }
            data.list.add(h);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Hideout h : this.list) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", h.pos.asLong());
            c.putIntArray("levels", h.levels);
            c.putInt("building", h.building);
            c.putLong("buildDone", h.buildDone);
            c.putLong("buildStarted", h.buildStarted);
            for (Module m : Module.values()) {
                if (h.given[m.ordinal()] != null) {
                    c.putIntArray("given_" + m.name(), h.given[m.ordinal()]);
                }
            }
            list.add(c);
        }
        tag.put("hideouts", list);
        return tag;
    }
}
