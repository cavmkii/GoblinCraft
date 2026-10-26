package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.rpg.RpgHooks;
import net.minecraft.advancements.critereon.BrewedPotionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires when a player takes a finished potion out of a brewing stand. */
@Mixin(BrewedPotionTrigger.class)
public abstract class BrewedPotionTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void seancraft$brewed(ServerPlayer player, Holder<Potion> potion, CallbackInfo ci) {
        RpgHooks.onBrewed(player);
    }
}
