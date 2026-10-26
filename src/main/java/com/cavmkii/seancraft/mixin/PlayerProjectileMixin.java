package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.tetris.Tetris;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bows and crossbows draw arrows from the rig, pockets and backpack when the hotbar has none. */
@Mixin(Player.class)
public abstract class PlayerProjectileMixin {
    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void seancraft$gridAmmo(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue().isEmpty() && weapon.getItem() instanceof ProjectileWeaponItem bow) {
            ItemStack found = Tetris.gridProjectile((Player) (Object) this, bow.getAllSupportedProjectiles());
            if (!found.isEmpty()) {
                cir.setReturnValue(found);
            }
        }
    }
}
