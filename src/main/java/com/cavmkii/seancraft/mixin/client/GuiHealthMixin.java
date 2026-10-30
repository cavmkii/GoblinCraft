package com.cavmkii.seancraft.mixin.client;

import com.cavmkii.seancraft.client.tarkov.HealthBar;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player health is 300 (design doc 5.1), which vanilla would draw as fifteen rows of hearts. One bar takes the hearts'
 * place instead, and the armor icons sit where they would above a single row.
 */
@Mixin(Gui.class)
abstract class GuiHealthMixin {
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void seancraft$healthBar(GuiGraphics g, Player player, int x, int y, int rowHeight, int regenIndex, float maxHealth,
            int health, int displayHealth, int absorption, boolean blinking, CallbackInfo ci) {
        HealthBar.render(g, player, x, y, maxHealth, absorption, blinking);
        ci.cancel();
    }

    /** No hunger bar: food slots show with the effects instead (design doc 11). */
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void seancraft$noHunger(GuiGraphics g, Player player, int y, int x, CallbackInfo ci) {
        ci.cancel();
    }

    /** The number of heart rows, which places the armor bar: always one. */
    @Redirect(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;ceil(F)I", ordinal = 2))
    private int seancraft$oneRow(float rows) {
        return Math.min(1, Mth.ceil(rows));
    }
}
