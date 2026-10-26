package com.cavmkii.seancraft.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The vanilla effect icons (top right, no time left) give way to the bottom-left effect HUD. */
@Mixin(Gui.class)
abstract class GuiEffectsMixin {
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void seancraft$hideVanillaEffects(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        ci.cancel();
    }
}
