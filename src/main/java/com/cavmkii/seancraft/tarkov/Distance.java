package com.cavmkii.seancraft.tarkov;

import com.cavmkii.seancraft.Cfg;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Danger rises with distance from world spawn (design doc 2.3.1). Measured horizontally; in the Nether, in
 * overworld-equivalent blocks (coordinates x 8), so a Nether shortcut meets the band of the land it skips.
 */
public enum Distance {
    HEARTLAND("Heartland", 0, 0, 0, 0, false),
    WILDS("Wilds", 1000, 0.25, 0.15, 0.25, false),
    FRONTIER("Frontier", 3000, 0.50, 0.30, 0.50, true),
    FAR_LANDS("Far lands", 6000, 1.00, 0.50, 1.00, true);

    /** Threat and distance bonuses add, and together never pass these (2.3.2). */
    public static final double MAX_HEALTH_MULTIPLIER = 4;
    public static final double MAX_DAMAGE_MULTIPLIER = 2.5;
    /** Raid minute a raid starts at in the Frontier, the Far lands and the Nether: threat level 2. */
    public static final double HEAD_START_MINUTES = 20;

    public final String label;
    public final double from;
    public final double health;
    public final double damage;
    public final double rewards;
    public final boolean headStart;

    Distance(String label, double from, double health, double damage, double rewards, boolean headStart) {
        this.label = label;
        this.from = from;
        this.health = health;
        this.damage = damage;
        this.rewards = rewards;
        this.headStart = headStart;
    }

    public static double blocksFromSpawn(ServerLevel level, Vec3 pos) {
        BlockPos spawn = level.getServer().overworld().getSharedSpawnPos();
        double scale = level.dimension() == Level.NETHER ? 8 : 1;
        double dx = pos.x * scale - spawn.getX();
        double dz = pos.z * scale - spawn.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Distance at(ServerLevel level, Vec3 pos) {
        if (!Cfg.distanceScaling) {
            return HEARTLAND;
        }
        double d = blocksFromSpawn(level, pos);
        Distance band = HEARTLAND;
        for (Distance b : values()) {
            if (d >= b.from) {
                band = b;
            }
        }
        return band;
    }
}
