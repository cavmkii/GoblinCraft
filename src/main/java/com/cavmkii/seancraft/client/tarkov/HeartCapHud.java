package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tarkov.Body;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Hearts that belong to destroyed body parts can't be healed (food, potions, meds) until surgery restores the
 * part, and lasting damage from a surgical kit locks part of that part's hearts until a surgery case. They're
 * drawn over the vanilla hearts as grey, crossed-out hearts.
 */
public final class HeartCapHud {
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");

    private HeartCapHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator() || mc.gameMode == null) {
            return;
        }
        float capFraction = Body.capFraction(ClientTarkov.body.body(), ClientTarkov.body.scar());
        if (capFraction >= 0.999f) {
            return;
        }
        // Same layout as vanilla Gui.renderPlayerHealth.
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        float cap = player.getMaxHealth() * capFraction;
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int left = g.guiWidth() / 2 - 91;
        int top = g.guiHeight() - 39;
        int hearts = Mth.ceil(maxHealth / 2.0F);
        g.pose().pushPose();
        // Vanilla's HUD layers each sit higher than the last; stay above the hearts layer.
        g.pose().translate(0, 0, 2000);
        for (int i = 0; i < hearts; i++) {
            // A heart is locked once more than half of it lies above the cap.
            if (i * 2 + 1 <= cap) {
                continue;
            }
            int x = left + (i % 10) * 8;
            int y = top - (i / 10) * rowHeight;
            RenderSystem.setShaderColor(0.32f, 0.32f, 0.34f, 1f);
            g.blitSprite(HEART, x, y, 9, 9);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            // Sprites and fills are batched separately; flush so the cross lands on top of the heart.
            g.flush();
            for (int k = 1; k < 8; k++) {
                g.fill(x + k, y + k, x + k + 1, y + k + 1, 0xFFD03A3A);
                g.fill(x + 8 - k, y + k, x + 9 - k, y + k + 1, 0xFFD03A3A);
            }
        }
        g.pose().popPose();
    }
}
