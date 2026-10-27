package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.carry.ClientCarry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Someone carrying a chest holds both arms out in front, under it. */
@Mixin(HumanoidModel.class)
abstract class HumanoidModelCarryMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void seancraft$carryArms(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, CallbackInfo ci) {
        if (entity instanceof Player && ClientCarry.of(entity) != null) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            model.rightArm.xRot = -1.25f;
            model.leftArm.xRot = -1.25f;
            model.rightArm.yRot = -0.3f;
            model.leftArm.yRot = 0.3f;
            model.rightArm.zRot = 0;
            model.leftArm.zRot = 0;
        }
    }
}
