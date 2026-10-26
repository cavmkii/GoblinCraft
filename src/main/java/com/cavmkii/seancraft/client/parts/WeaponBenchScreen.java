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
    /** Build picker: icon buttons under the item slot, 3 per row. */
    private static final int PICK_X = 4;
    private static final int PICK_Y = 56;
    private static final int ASSEMBLE_X = 112;
    private static final int ASSEMBLE_Y = 100;
    private static final net.minecraft.world.item.Item[] PICK_ICONS = {
            net.minecraft.world.item.Items.IRON_SWORD, net.minecraft.world.item.Items.IRON_AXE, net.minecraft.world.item.Items.CROSSBOW,
            net.minecraft.world.item.Items.SHIELD, net.minecraft.world.item.Items.IRON_HELMET, net.minecraft.world.item.Items.IRON_CHESTPLATE,
            net.minecraft.world.item.Items.IRON_LEGGINGS, net.minecraft.world.item.Items.IRON_BOOTS};
    private static final String[] PICK_NAMES = {"Sword", "Axe", "Crossbow", "Shield", "Helmet", "Chestplate", "Leggings", "Boots"};

    private int pickX(int i) {
        return leftPos + PICK_X + (i % 3) * 18;
    }

    private int pickY(int i) {
        return topPos + PICK_Y + (i / 3) * 18;
    }

    public WeaponBenchScreen(WeaponBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 212;
        inventoryLabelY = 119;
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
            String name = family.slotName(slot);
            if (menu.buildMode() && slot == PartSlot.PLATE) {
                name += " x" + WeaponBenchMenu.platesFor(family);
            }
            g.drawString(font, name, x + WeaponBenchMenu.PART_X + 20, sy + 1, 0xFF404040, false);
            ItemStack part = menu.slots.get(1 + i).getItem();
            if (part.getItem() instanceof PartItem p && !menu.buildMode()) {
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

    private void renderBuild(GuiGraphics g, int mouseX, int mouseY) {
        if (!menu.buildMode()) {
            return;
        }
        for (int i = 0; i < PICK_ICONS.length; i++) {
            int px = pickX(i);
            int py = pickY(i);
            boolean selected = menu.buildingIndex() == i;
            boolean hover = mouseX >= px && mouseX < px + 17 && mouseY >= py && mouseY < py + 17;
            g.fill(px, py, px + 17, py + 17, selected ? 0xFF3A6A3A : hover ? 0xFF9A9AAA : 0xFF7A7A88);
            g.renderItem(new ItemStack(PICK_ICONS[i]), px, py);
        }
        ItemStack preview = menu.assemblyResult();
        boolean ready = !preview.isEmpty();
        if (ready) {
            // Ghost of the result in the item slot.
            g.renderItem(preview, leftPos + WeaponBenchMenu.ITEM_X, topPos + WeaponBenchMenu.ITEM_Y);
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);
            g.fill(leftPos + WeaponBenchMenu.ITEM_X, topPos + WeaponBenchMenu.ITEM_Y, leftPos + WeaponBenchMenu.ITEM_X + 16,
                    topPos + WeaponBenchMenu.ITEM_Y + 16, 0x80C6C6C6);
            g.pose().popPose();
        }
        if (menu.family() != null) {
            int ax = leftPos + ASSEMBLE_X;
            int ay = topPos + ASSEMBLE_Y;
            boolean hover = ready && mouseX >= ax && mouseX < ax + 58 && mouseY >= ay && mouseY < ay + 14;
            g.fill(ax, ay, ax + 58, ay + 14, !ready ? 0xFF6A6A70 : hover ? 0xFF4C8C4C : 0xFF3A6A3A);
            g.drawString(font, "ASSEMBLE", ax + (58 - font.width("ASSEMBLE")) / 2, ay + 3, ready ? 0xFFFFFFFF : 0xFFAAAAAA, false);
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
            int ly = 46;
            g.pose().pushPose();
            g.pose().scale(0.75f, 0.75f, 1);
            for (String line : lines) {
                g.drawString(font, line, 8, (int) (ly / 0.75f), 0xFF303030, false);
                ly += 8;
            }
            g.pose().popPose();
        } else {
            g.pose().pushPose();
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, "Modify: put item in", (int) (6 / 0.75f), (int) (42 / 0.75f), 0xFF505050, false);
            g.drawString(font, "Build: pick below", (int) (6 / 0.75f), (int) (48 / 0.75f), 0xFF505050, false);
            g.pose().popPose();
        }
    }

    private static String pct(String label, float value) {
        return String.format("%s %+d%%", label, Math.round(value * 100));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderBuild(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
        if (menu.buildMode()) {
            for (int i = 0; i < PICK_ICONS.length; i++) {
                if (mouseX >= pickX(i) && mouseX < pickX(i) + 17 && mouseY >= pickY(i) && mouseY < pickY(i) + 17) {
                    g.renderTooltip(font, Component.literal("Build a " + PICK_NAMES[i].toLowerCase()), mouseX, mouseY);
                }
            }
        }
        for (int i = 0; i < WeaponBenchMenu.PARTS; i++) {
            ItemStack part = menu.slotFor(i) != null ? menu.slots.get(1 + i).getItem() : ItemStack.EMPTY;
            int sy = topPos + WeaponBenchMenu.PART_Y + i * 20;
            if (part.getItem() instanceof PartItem p && !menu.buildMode() && Parts.level(part) < PartDef.MAX_LEVEL
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
        if (menu.buildMode()) {
            for (int i = 0; i < PICK_ICONS.length; i++) {
                if (mouseX >= pickX(i) && mouseX < pickX(i) + 17 && mouseY >= pickY(i) && mouseY < pickY(i) + 17) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WeaponBenchMenu.PICK_BUTTON + i);
                    return true;
                }
            }
            int ax = leftPos + ASSEMBLE_X;
            int ay = topPos + ASSEMBLE_Y;
            if (mouseX >= ax && mouseX < ax + 58 && mouseY >= ay && mouseY < ay + 14 && !menu.assemblyResult().isEmpty()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WeaponBenchMenu.ASSEMBLE_BUTTON);
                return true;
            }
        }
        for (int i = 0; i < WeaponBenchMenu.PARTS; i++) {
            int sy = topPos + WeaponBenchMenu.PART_Y + i * 20;
            ItemStack part = menu.slotFor(i) != null ? menu.slots.get(1 + i).getItem() : ItemStack.EMPTY;
            if (part.getItem() instanceof PartItem && !menu.buildMode() && Parts.level(part) < PartDef.MAX_LEVEL
                    && mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 22 && mouseY >= sy && mouseY < sy + 16) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
