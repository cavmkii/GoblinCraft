package com.cavmkii.seancraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

/**
 * When it starts snowing where you are, your character says a line in chat, formatted like ordinary
 * player chat. Once per snowfall, with a cooldown.
 */
public final class SnowQuip {
    private static final Map<UUID, Boolean> WAS_SNOWING = new HashMap<>();
    private static final Map<UUID, Long> LAST_SAID = new HashMap<>();

    private SnowQuip() {}

    public static void tick(MinecraftServer server) {
        if (!Cfg.snowLineEnabled || server.getTickCount() % 40 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            UUID id = player.getUUID();
            boolean snowing = isSnowingAt(player.serverLevel(), player.blockPosition());
            boolean was = WAS_SNOWING.getOrDefault(id, false);
            long now = server.getTickCount();
            Long last = LAST_SAID.get(id);
            boolean cooledDown = last == null || now - last > (long) (Cfg.snowLineCooldownMinutes * 60 * 20);
            if (snowing && !was && cooledDown) {
                LAST_SAID.put(id, now);
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("chat.type.text", player.getDisplayName(), Component.literal(Cfg.snowLine)), false);
            }
            WAS_SNOWING.put(id, snowing);
        }
    }

    private static boolean isSnowingAt(ServerLevel level, BlockPos pos) {
        if (!level.isRaining() || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            return false;
        }
        return level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }

    public static void reset() {
        WAS_SNOWING.clear();
        LAST_SAID.clear();
    }
}
