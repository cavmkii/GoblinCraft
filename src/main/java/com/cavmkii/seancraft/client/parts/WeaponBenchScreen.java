package com.cavmkii.seancraft.client.parts;

import com.cavmkii.seancraft.parts.Family;
import com.cavmkii.seancraft.parts.PartDef;
import com.cavmkii.seancraft.parts.PartItem;
import com.cavmkii.seancraft.parts.PartSlot;
import com.cavmkii.seancraft.parts.Parts;
import com.cavmkii.seancraft.parts.WeaponBenchMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Weapon Bench screen, drawn with plain rectangles. */
public class WeaponBenchScreen extends AbstractContainerScreen<WeaponBenchMenu> {
    private static final int BUTTON_X = 146;

    public WeaponBenchScreen(WeaponBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelY = 97;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean bench = slot.index <= WeaponBenchMenu.PARTS;
            g.fill(sx, sy, sx + 18, sy + 18, bench ? 0xFF4A4A55 : 0xFF373737);
            g.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, bench ? 0xFF7A7A88 : 0xFF8B8B8B);
        }
        Family family = menu.family();
        for (int i = 0; i < WeaponBenchMenu.PARTS; i++) {
            PartSlot slot = menu.slotFor(i);
            if (slot == null) {
                continue;
            }
            int sy = y + WeaponBenchMenu.PART_Y + i * 20;
            g.drawString(font, family.slotName(slot), x + WeaponBenchMenu.PART_X + 20, sy + 1, 0xFF404040, false);
            ItemStack part = menu.slots.get(1 + i).getItem();
            if (part.getItem() instanceof PartItem p) {
                int level = Parts.level(part);
                g.drawString(font, "L" + level, x + WeaponBenchMenu.PART_X + 20, sy + 10, 0xFF606060, false);
                if (level < PartDef.MAX_LEVEL) {
                    boolean hover = mouseX >= x + BUTTON_X && mouseX < x + BUTTON_X + 22 && mouseY >= sy && mouseY < sy + 16;
                    g.fill(x + BUTTON_X, sy, x + BUTTON_X + 22, sy + 16, hover ? 0xFF4C8C4C : 0xFF3A6A3A);
                    g.drawString(font, "▲", x + BUTTON_X + 8, sy + 4, 0xFFFFFFFF, false);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        ItemStack weapon = menu.weapon();
        if (Parts.modular(weapon)) {
            Parts.Totals t = Parts.totals(weapon);
            List<String> lines = new ArrayList<>();
            Family family = menu.family();
            if (family.armor()) {
                lines.add(pct("Armor", t.armor));
            } else {
                lines.add(pct("Damage", t.damage));
                if (family != Family.BOW && family != Family.CROSSBOW && family != Family.SHIELD) {
                    lines.add(pct("Speed", -t.stamina));
                }
            }
            lines.add(pct("Durab.", t.durability));
            int ly = 64;
            g.pose().pushPose();
            g.pose().scale(0.75f, 0.75f, 1);
            for (String line : lines) {
                g.drawString(font, line, 8, (int) (ly / 0.75f), 0xFF303030, false);
                ly += 8;
            }
            g.pose().popPose();
        } else {
            g.drawString(font, "Put a weapon", 6, 64, 0xFF606060, false);
            g.drawString(font, "or armor here", 6, 73, 0xFF606060, false);
        }
    }

    private static String pct(String label, float value) {
        return String.format("%s %+d%%", label, Math.round(value * 100));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        for (int i = 0; i < WeaponBenchMenu.PARTS; i++) {
            ItemStack part = menu.slotFor(i) != null ? menu.slots.get(1 + i).getItem() : ItemStack.EMPTY;
            int sy = topPos + WeaponBenchMenu.PART_Y + i * 20;
            if (part.getItem() instanceof PartItem p && Parts.level(part) < PartDef.MAX_LEVEL
                    && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 22 && mouseY >= sy && mouseY < sy + 16) {
                int level = Parts.level(part);
                g.renderTooltip(font, List.of(Component.literal("Level up to " + (level + 1)),
                        Component.literal("Costs " + p.def.upgradeCost(level) + " " + new ItemStack(p.def.material()).getHoverName().getString())),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < WeaponBenchMenu.PARTS; i++) {
            int sy = topPos + WeaponBenchMenu.PART_Y + i * 20;
            ItemStack part = menu.slotFor(i) != null ? menu.slots.get(1 + i).getItem() : ItemStack.EMPTY;
            if (part.getItem() instanceof PartItem && Parts.level(part) < PartDef.MAX_LEVEL
                    && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 22 && mouseY >= sy && mouseY < sy + 16) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
