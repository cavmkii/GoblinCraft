package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.food.Food;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A finished meal goes into a food slot. */
@Mixin(Player.class)
abstract class PlayerEatMixin {
    @Inject(method = "eat", at = @At("HEAD"))
    private void seancraft$meal(Level level, ItemStack food, FoodProperties props, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            Food.eaten(player, food);
        }
    }
}
