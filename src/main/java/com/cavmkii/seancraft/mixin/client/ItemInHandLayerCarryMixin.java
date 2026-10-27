package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.carry.ClientCarry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands full: someone carrying a chest isn't shown holding their item too. */
@Mixin(ItemInHandLayer.class)
abstract class ItemInHandLayerCarryMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void seancraft$handsFull(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ClientCarry.of(entity) != null) {
            ci.cancel();
        }
    }
}
