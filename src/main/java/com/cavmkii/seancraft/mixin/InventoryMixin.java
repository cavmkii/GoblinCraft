package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.tetris.Tetris;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pickups and gifts go to the Tetris grids instead of the vanilla main slots. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void seancraft$tetrisPickup(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot == -1 && player instanceof ServerPlayer sp && Tetris.active(sp)) {
            cir.setReturnValue(Tetris.pickup(sp, stack));
        }
    }
}
