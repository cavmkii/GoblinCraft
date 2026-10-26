package com.cavmkii.seancraft;

import com.cavmkii.seancraft.entity.Wander;
import com.cavmkii.seancraft.world.WorldDirector;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * /seancraft where | summon joe|sean | babywars start|stop | reload
 */
public final class GoblinCommands {
    private GoblinCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seancraft")
                .then(Commands.literal("where").executes(ctx -> reply(ctx, director(ctx).describe())))
                .then(Commands.literal("summon").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("joe").executes(ctx -> summon(ctx, WorldDirector.Kind.JOE)))
                        .then(Commands.literal("sean").executes(ctx -> summon(ctx, WorldDirector.Kind.SEAN))))
                .then(Commands.literal("babywars").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("start").executes(GoblinCommands::startWar))
                        .then(Commands.literal("stop").executes(ctx -> reply(ctx,
                                director(ctx).war.end("An admin called off the Baby Wars.") ? "Calling off the Baby Wars." : "No war is running."))))
                .then(Commands.literal("reload").requires(src -> src.hasPermission(2))
                        .executes(ctx -> reply(ctx, Cfg.load() + "\n" + JoeLines.load()))));
    }

    private static WorldDirector director(CommandContext<CommandSourceStack> ctx) {
        return WorldDirector.get();
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    /** Replace Joe or Sean with a fresh one a few blocks in front of you. */
    private static int summon(CommandContext<CommandSourceStack> ctx, WorldDirector.Kind kind) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null || player.level() != director(ctx).overworld()) {
            ctx.getSource().sendFailure(Component.literal("Run this as a player in the overworld."));
            return 0;
        }
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize().scale(kind == WorldDirector.Kind.JOE ? 4 : 7);
        // Stand it on the ground, not at the player's height: on a slope that's inside a block.
        BlockPos spot = Wander.surface(player.serverLevel(), player.position().add(look));
        director(ctx).replace(kind, spot != null ? spot : player.blockPosition());
        return reply(ctx, "Summoning " + (kind == WorldDirector.Kind.JOE ? "Joe" : "Sean") + ".");
    }

    private static int startWar(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        WorldDirector director = director(ctx);
        if (player == null || player.level() != director.overworld()) {
            ctx.getSource().sendFailure(Component.literal("Run this as a player in the overworld."));
            return 0;
        }
        if (director.war.isActive()) {
            return reply(ctx, "The Baby Wars are already on.");
        }
        director.war.forceStart(player);
        return reply(ctx, "The Baby Wars begin...");
    }
}
