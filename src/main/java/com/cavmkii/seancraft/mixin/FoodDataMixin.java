package com.cavmkii.seancraft.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla hunger is gone (design doc 11): the bar is held just short of full, so you can always eat, never starve and
 * get no regeneration from it. Meals work through food slots instead.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void seancraft$noHunger(Player player, CallbackInfo ci) {
        foodLevel = 17;
        saturationLevel = 0;
        exhaustionLevel = 0;
        ci.cancel();
    }

    @Inject(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At("HEAD"), cancellable = true)
    private void seancraft$noNutrition(FoodProperties food, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true)
    private void seancraft$noNutrition(int nutrition, float saturation, CallbackInfo ci) {
        ci.cancel();
    }
}
