package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.rpg.RpgHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "onEnchantmentPerformed", at = @At("TAIL"))
    private void seancraft$enchanted(ItemStack stack, int levels, CallbackInfo ci) {
        RpgHooks.onEnchanted((Player) (Object) this, levels);
    }
}
