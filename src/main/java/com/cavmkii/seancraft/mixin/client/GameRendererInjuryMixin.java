package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.tarkov.InjuryCamera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Limp, sway and aim wobble from injuries. bobHurt runs every frame, for the view and the hand, bobbing on or off. */
@Mixin(GameRenderer.class)
abstract class GameRendererInjuryMixin {
    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void seancraft$injuries(PoseStack pose, float partialTick, CallbackInfo ci) {
        InjuryCamera.apply(pose, partialTick);
    }
}
