package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.tarkov.Tarkov;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raid death rules run just before a dead player's inventory drops. */
@Mixin(Player.class)
public abstract class PlayerDeathMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void seancraft$raidDeath(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            Tarkov.beforeDeathDrops(player);
        }
    }
}
