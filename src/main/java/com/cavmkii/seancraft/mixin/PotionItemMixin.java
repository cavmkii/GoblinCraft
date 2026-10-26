package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.rpg.RpgHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the window in which a drunk potion applies its effects, for the Brewing bonus. */
@Mixin(PotionItem.class)
public abstract class PotionItemMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void seancraft$startDrinking(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        RpgHooks.drinking = entity;
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void seancraft$stopDrinking(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        RpgHooks.drinking = null;
    }
}
