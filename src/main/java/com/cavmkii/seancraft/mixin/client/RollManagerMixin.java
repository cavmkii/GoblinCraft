package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.combat.ClientCombat;
import net.combat_roll.internals.RollManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Combat Roll's roll, paid for in stamina: no roll without the stamina for it, or in the middle of a swing. */
@Mixin(value = RollManager.class, remap = false)
abstract class RollManagerMixin {
    @Inject(method = "isRollAvailable", at = @At("HEAD"), cancellable = true)
    private void seancraft$needsStamina(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!ClientCombat.canRoll()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onRoll", at = @At("HEAD"))
    private void seancraft$payForRoll(LocalPlayer player, CallbackInfo ci) {
        ClientCombat.rolled();
    }
}
