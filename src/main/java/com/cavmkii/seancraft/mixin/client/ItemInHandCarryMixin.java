package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.carry.ClientCarry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While carrying a chest or barrel, first person shows it in your hands instead of the held item. */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandCarryMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void seancraft$carried(float partialTick, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light,
            CallbackInfo ci) {
        if (ClientCarry.renderFirstPerson(pose, buffers, light)) {
            ci.cancel();
        }
    }
}
