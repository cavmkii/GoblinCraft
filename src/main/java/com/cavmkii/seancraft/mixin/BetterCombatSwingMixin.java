package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.combat.Combat;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Better Combat tells the server when a swing starts, so others see it; that is where the swing's stamina is paid. */
@Mixin(value = ServerNetwork.class, remap = false)
abstract class BetterCombatSwingMixin {
    @Inject(method = "handleAttackAnimation", at = @At("HEAD"))
    private static void seancraft$payForSwing(Packets.AttackAnimation packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        Combat.onSwing(player);
    }
}
