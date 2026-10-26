package com.cavmkii.goblincraft.client.tarkov;

import com.cavmkii.goblincraft.tarkov.SecureMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Drawn with plain rectangles so it needs no texture. */
public class SecureScreen extends AbstractContainerScreen<SecureMenu> {
    public SecureScreen(SecureMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = SecureMenu.topHeight(menu.tier) + 76 + 6;
        inventoryLabelY = SecureMenu.topHeight(menu.tier) - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        g.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean secure = slot.index < menu.tier.slots();
            g.fill(sx, sy, sx + 18, sy + 18, secure ? 0xFF6B4A1F : 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, secure ? 0xFF9C7A45 : 0xFF8B8B8B);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
