package com.cavmkii.goblincraft;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * "Player level" as a 0..1 value. Minecraft has no skills, so the two stand-ins are:
 * SKILLS, the highest XP level among players near the position; and BOSSES, how many of a list of
 * progression advancements (iron, the Nether, blaze rods, the Stronghold, the Dragon, the Wither, netherite)
 * any online player has earned.
 */
public final class PowerLevel {
    private static final double SCAN_RADIUS = 80;

    private static float cachedBosses;
    private static long nextBossCheck;

    private PowerLevel() {}

    public static float evaluate(ServerLevel level, Vec3 position) {
        float bosses = bossFactor(level.getServer(), level.getGameTime());
        float skills = skillFactor(level, position);
        return switch (Cfg.scaling) {
            case BOSSES -> bosses;
            case SKILLS -> skills;
            case HIGHEST -> Math.max(bosses, skills);
        };
    }

    private static float skillFactor(ServerLevel level, Vec3 position) {
        int best = 0;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.position().distanceToSqr(position) <= SCAN_RADIUS * SCAN_RADIUS) {
                best = Math.max(best, player.experienceLevel);
            }
        }
        return Mth.clamp(best / (float) Math.max(1, Cfg.skillsMaxLevel), 0f, 1f);
    }

    private static float bossFactor(MinecraftServer server, long now) {
        // The second check catches a fresh world in the same game session (game time starts over).
        if (now < nextBossCheck && now >= nextBossCheck - 300) {
            return cachedBosses;
        }
        nextBossCheck = now + 300;

        String[] ids = Cfg.bossAdvancements.split(",");
        int total = 0;
        int done = 0;
        for (String raw : ids) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            total++;
            ResourceLocation location = ResourceLocation.tryParse(id);
            AdvancementHolder advancement = location != null ? server.getAdvancements().get(location) : null;
            if (advancement == null) {
                continue;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                    done++;
                    break;
                }
            }
        }
        cachedBosses = total == 0 ? 0f : done / (float) total;
        return cachedBosses;
    }

    public static float lerp(double min, double max, float t) {
        return (float) (min + (max - min) * t);
    }
}
