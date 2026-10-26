package com.cavmkii.seancraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * NPC speech and on-screen notices. Minecraft has no speech bubbles, so lines go to the chat of
 * players in earshot; a shout carries further.
 */
public final class Speech {
    public static final double SAY_RANGE = 40;
    public static final double SHOUT_RANGE = 100;

    private Speech() {}

    public static void say(Entity speaker, String name, ChatFormatting color, String line) {
        say(speaker, name, color, line, false);
    }

    public static void say(Entity speaker, String name, ChatFormatting color, String line, boolean shout) {
        if (!(speaker.level() instanceof ServerLevel level)) {
            return;
        }
        MutableComponent text = Component.literal(line);
        if (shout) {
            text = text.withStyle(ChatFormatting.BOLD);
        }
        Component message = Component.literal("<" + name + "> ").withStyle(color).append(text);
        double range = shout ? SHOUT_RANGE : SAY_RANGE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(speaker) <= range * range) {
                player.sendSystemMessage(message);
            }
        }
    }

    /** Action-bar notice for players near a point. */
    public static void announce(ServerLevel level, Vec3 pos, double range, String text) {
        Component message = Component.literal(text).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos) <= range * range) {
                player.displayClientMessage(message, true);
            }
        }
    }

    public static void notify(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true);
    }

    /** Big centre-screen title, plus a chat copy so it isn't missed. */
    public static void title(ServerPlayer player, String text) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    public static void titleAll(MinecraftServer server, String text) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            title(player, text);
        }
    }
}
