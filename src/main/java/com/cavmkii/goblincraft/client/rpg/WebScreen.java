package com.cavmkii.goblincraft.client.rpg;

import com.cavmkii.goblincraft.rpg.ActiveSkill;
import com.cavmkii.goblincraft.rpg.Archetype;
import com.cavmkii.goblincraft.rpg.Attr;
import com.cavmkii.goblincraft.rpg.PassiveWeb;
import com.cavmkii.goblincraft.rpg.PlayerData;
import com.cavmkii.goblincraft.rpg.RpgNet;
import com.cavmkii.goblincraft.rpg.Skill;
import com.cavmkii.goblincraft.rpg.Stats;
import com.cavmkii.goblincraft.rpg.WebNode;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The passive web. Drag to pan, scroll to zoom, click a connected node to take it. Click a learned
 * active skill, then an ability slot at the bottom, to slot it; right-click a slot to empty it.
 */
public class WebScreen extends Screen {
    private static final int PANEL = 128;
    private static final int SLOT = 20;

    private double panX;
    private double panY;
    private double zoom = 1;
    private boolean dragging;
    private double pressX;
    private double pressY;
    private ActiveSkill selected;

    public WebScreen() {
        super(Component.literal("Passive Web"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private double scale() {
        double fit = Math.min(width - PANEL - 10, height - SLOT - 20) / 2.0 / 8.4;
        return fit * zoom;
    }

    private double centerX() {
        return PANEL + (width - PANEL) / 2.0 + panX;
    }

    private double centerY() {
        return (height - SLOT - 8) / 2.0 + panY;
    }

    private double sx(WebNode n) {
        return centerX() + n.x * scale();
    }

    private double sy(WebNode n) {
        return centerY() + n.y * scale();
    }

    private double size(WebNode n) {
        double base = switch (n.kind) {
            case START -> 5;
            case SMALL -> 2.5;
            case NOTABLE -> 3.5;
            case ACTIVE -> 4.5;
            case KEYSTONE -> 5.5;
        };
        return Math.max(1.5, base * scale() / 12);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF0101014);
        PlayerData data = ClientRpg.data;

        // Links.
        for (WebNode n : PassiveWeb.NODES) {
            for (int linkId : n.links) {
                if (linkId < n.id) {
                    continue;
                }
                WebNode m = PassiveWeb.get(linkId);
                boolean a = data.isAllocated(n.id);
                boolean b = data.isAllocated(m.id);
                int color = a && b ? 0xFFE0C060 : a || b ? 0xFF8A8A8A : 0xFF45454C;
                line(g, sx(n), sy(n), sx(m), sy(m), color);
            }
        }

        // Nodes.
        WebNode hovered = null;
        for (WebNode n : PassiveWeb.NODES) {
            double x = sx(n);
            double y = sy(n);
            double r = size(n);
            if (x < PANEL - r || x > width + r || y < -r || y > height + r) {
                continue;
            }
            boolean allocated = data.isAllocated(n.id);
            boolean available = data.canAllocate(n.id);
            int color = n.archetype != null ? n.archetype.color : 0xFFDDDDDD;
            int border = allocated ? 0xFFFFE080 : available ? 0xFFFFFFFF : 0xFF4A4A50;
            int fill = allocated ? color : (color & 0x00FFFFFF) | (available ? 0x90000000 : 0x40000000);
            if (n.active != null && n.active == selected) {
                border = 0xFF55FFFF;
            }
            int x0 = (int) Math.round(x - r);
            int y0 = (int) Math.round(y - r);
            int x1 = (int) Math.round(x + r);
            int y1 = (int) Math.round(y + r);
            g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, border);
            g.fill(x0, y0, x1, y1, 0xFF15151A);
            g.fill(x0, y0, x1, y1, fill);
            if (n.active != null && r >= 4) {
                g.pose().pushPose();
                float s = (float) (r * 2 / 16.0);
                g.pose().translate(x - r, y - r, 0);
                g.pose().scale(s, s, 1);
                g.renderItem(new ItemStack(n.active.icon), 0, 0);
                g.pose().popPose();
            }
            if (Math.abs(mouseX - x) <= r + 1 && Math.abs(mouseY - y) <= r + 1) {
                hovered = n;
            }
        }

        renderPanel(g, data);
        int hoveredSlot = renderSlots(g, data, mouseX, mouseY);

        if (hovered != null && mouseX > PANEL) {
            g.renderTooltip(font, tooltip(hovered, data), Optional.empty(), mouseX, mouseY);
        } else if (hoveredSlot >= 0) {
            ActiveSkill skill = data.slot(hoveredSlot);
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot >= data.unlockedSlots()) {
                lines.add(Component.literal("Unlocks at level " + PlayerData.SLOT_LEVELS[hoveredSlot]).withStyle(ChatFormatting.GRAY));
            } else if (skill != null) {
                lines.add(Component.literal(skill.label).withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal(skill.description).withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("Right-click to empty").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.literal("Empty slot").withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("Click a learned skill on the web, then this slot").withStyle(ChatFormatting.DARK_GRAY));
            }
            g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> tooltip(WebNode n, PlayerData data) {
        List<Component> lines = new ArrayList<>();
        ChatFormatting nameColor = switch (n.kind) {
            case KEYSTONE -> ChatFormatting.GOLD;
            case NOTABLE -> ChatFormatting.YELLOW;
            case ACTIVE -> ChatFormatting.AQUA;
            default -> ChatFormatting.WHITE;
        };
        lines.add(Component.literal(n.name).withStyle(nameColor));
        String kind = switch (n.kind) {
            case START -> "Where every path begins";
            case SMALL -> "Passive";
            case NOTABLE -> "Notable passive";
            case ACTIVE -> "Active skill";
            case KEYSTONE -> "Keystone";
        };
        lines.add(Component.literal(kind + (n.archetype != null ? " - " + n.archetype.label : "")).withStyle(ChatFormatting.DARK_GRAY));
        for (String line : n.describe()) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        if (n.kind != WebNode.Kind.START) {
            if (data.isAllocated(n.id)) {
                lines.add(Component.literal(n.active != null ? "Learned. Click, then click a slot below." : "Allocated")
                        .withStyle(ChatFormatting.GREEN));
            } else if (data.canAllocate(n.id)) {
                lines.add(Component.literal("Click to allocate").withStyle(ChatFormatting.GREEN));
            } else if (data.freePoints() <= 0) {
                lines.add(Component.literal("No points left").withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.literal("Not connected to your web").withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    private void renderPanel(GuiGraphics g, PlayerData data) {
        g.fill(0, 0, PANEL, height, 0xFF1A1A20);
        g.fill(PANEL - 1, 0, PANEL, height, 0xFF3A3A44);
        Stats stats = ClientRpg.stats;
        float s = 0.75f;
        g.pose().pushPose();
        g.pose().scale(s, s, 1);
        int x = 6;
        int y = 6;
        int lh = 10;
        g.drawString(font, "Level " + data.level + (data.level >= PlayerData.MAX_LEVEL ? " (max)" : ""), x, y, 0xFFFFE080);
        y += lh;
        if (data.level < PlayerData.MAX_LEVEL) {
            long need = PlayerData.xpToNext(data.level);
            int w = (int) ((PANEL - 10) / s);
            g.fill(x, y, x + w, y + 4, 0xFF000000);
            g.fill(x, y, x + (int) (w * Math.min(1, data.xp / (double) need)), y + 4, 0xFF9B59B6);
            y += 6;
            g.drawString(font, data.xp + " / " + need + " xp", x, y, 0xFF999999);
            y += lh;
        }
        g.drawString(font, "Points: " + data.freePoints() + " free" + (data.lostPoints > 0 ? ", " + data.lostPoints + " lost" : ""), x, y,
                data.freePoints() > 0 ? 0xFF55FF55 : 0xFFAAAAAA);
        y += lh;
        g.drawString(font, "Path: " + path(data), x, y, 0xFFCCCCCC);
        y += lh + 4;

        for (Attr attr : Attr.values()) {
            String star = stats.primary == attr ? "  (primary)" : "";
            g.drawString(font, attr.label + ": " + String.format("%.1f", stats.attr(attr)) + star, x, y, 0xFF000000 | attr.color);
            y += lh;
        }
        g.drawString(font, String.format("+%.1f health, %.1f regen/s", stats.bonusHealth(), stats.healthRegen()), x, y, 0xFF999999);
        y += lh;
        g.drawString(font, String.format("+%.0f%% attack speed, +%.0f%% move", stats.attackSpeedPct(), stats.moveSpeedPct()), x, y, 0xFF999999);
        y += lh;
        g.drawString(font, String.format("+%.2f damage from %s", stats.primaryDamage(), stats.primary.label), x, y, 0xFF999999);
        y += lh;
        g.drawString(font, String.format("Mana %.0f  Rage %.0f  Stamina %.0f", stats.maxMana(), stats.maxRage(), stats.maxStamina()), x, y,
                0xFF999999);
        y += lh + 4;

        g.drawString(font, "Skills", x, y, 0xFFFFE080);
        y += lh;
        for (Skill skill : Skill.values()) {
            int level = data.skillLevel(skill);
            double xp = data.skillXp.getOrDefault(skill, 0.0);
            double from = Skill.xpForLevel(level);
            double to = Skill.xpForLevel(Math.min(Skill.MAX_LEVEL, level + 1));
            int w = 40;
            int bx = x + (int) ((PANEL - 8) / s) - w;
            g.drawString(font, skill.label, x, y, 0xFF000000 | skill.attr.color);
            g.drawString(font, String.valueOf(level), bx - 4 - font.width(String.valueOf(level)), y, 0xFFFFFFFF);
            g.fill(bx, y + 3, bx + w, y + 6, 0xFF000000);
            if (level < Skill.MAX_LEVEL && to > from) {
                g.fill(bx, y + 3, bx + (int) (w * Math.min(1, (xp - from) / (to - from))), y + 6, 0xFF000000 | skill.attr.color);
            }
            y += lh;
        }
        g.pose().popPose();
    }

    /** Which archetypes your web leans into, most first. */
    private static String path(PlayerData data) {
        Map<Archetype, Integer> counts = new EnumMap<>(Archetype.class);
        for (int id : data.nodes) {
            WebNode n = PassiveWeb.get(id);
            if (n != null && n.archetype != null) {
                counts.merge(n.archetype, 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return "none yet";
        }
        List<Map.Entry<Archetype, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, sorted.size()); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(sorted.get(i).getKey().label).append(' ').append(sorted.get(i).getValue());
        }
        return sb.toString();
    }

    private int slotX(int slot) {
        return (int) (PANEL + (width - PANEL) / 2.0 - PlayerData.SLOTS * SLOT / 2.0) + slot * SLOT;
    }

    private int slotY() {
        return height - SLOT - 4;
    }

    private int renderSlots(GuiGraphics g, PlayerData data, int mouseX, int mouseY) {
        int hovered = -1;
        int y = slotY();
        g.fill(slotX(0) - 2, y - 2, slotX(PlayerData.SLOTS - 1) + SLOT + 1, y + SLOT + 1, 0xC0000000);
        for (int s = 0; s < PlayerData.SLOTS; s++) {
            int x = slotX(s);
            boolean open = s < data.unlockedSlots();
            boolean hover = mouseX >= x && mouseX < x + SLOT - 1 && mouseY >= y && mouseY < y + SLOT - 1;
            if (hover) {
                hovered = s;
            }
            g.fill(x, y, x + SLOT - 1, y + SLOT - 1, hover && selected != null && open ? 0xFF55FFFF : 0xFF2A2A2A);
            g.fill(x + 1, y + 1, x + SLOT - 2, y + SLOT - 2, open ? 0xFF3C3C3C : 0xFF1C1C1C);
            ActiveSkill skill = data.slot(s);
            if (open && skill != null) {
                g.renderItem(new ItemStack(skill.icon), x + 1, y + 1);
            } else if (!open) {
                String need = String.valueOf(PlayerData.SLOT_LEVELS[s]);
                g.drawString(font, need, x + (SLOT - font.width(need)) / 2, y + 6, 0xFF666666, false);
            }
        }
        if (selected != null) {
            String hint = "Placing " + selected.label + ": click a slot";
            g.drawString(font, hint, slotX(0) + PlayerData.SLOTS * SLOT / 2 - font.width(hint) / 2, y - 12, 0xFF55FFFF);
        }
        return hovered;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        pressX = mouseX;
        pressY = mouseY;
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) > 3) {
            dragging = true;
        }
        if (dragging) {
            panX += dragX;
            panY += dragY;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging) {
            click(mouseX, mouseY, button);
        }
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double old = zoom;
        zoom = Math.max(0.6, Math.min(3.5, zoom * (scrollY > 0 ? 1.15 : 1 / 1.15)));
        // Zoom around the cursor.
        double factor = zoom / old;
        double cx = PANEL + (width - PANEL) / 2.0;
        double cy = (height - SLOT - 8) / 2.0;
        panX = (panX + cx - mouseX) * factor - (cx - mouseX);
        panY = (panY + cy - mouseY) * factor - (cy - mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientRpg.OPEN_WEB.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void click(double mouseX, double mouseY, int button) {
        PlayerData data = ClientRpg.data;
        int y = slotY();
        for (int s = 0; s < PlayerData.SLOTS; s++) {
            int x = slotX(s);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                if (s >= data.unlockedSlots()) {
                    return;
                }
                if (button == 1) {
                    ClientPlayNetworking.send(new RpgNet.SetSlot(s, ""));
                } else if (selected != null) {
                    ClientPlayNetworking.send(new RpgNet.SetSlot(s, selected.name()));
                    selected = null;
                }
                return;
            }
        }
        if (mouseX < PANEL) {
            return;
        }
        for (WebNode n : PassiveWeb.NODES) {
            double r = size(n) + 1;
            if (Math.abs(mouseX - sx(n)) <= r && Math.abs(mouseY - sy(n)) <= r) {
                if (data.canAllocate(n.id) && button == 0) {
                    ClientPlayNetworking.send(new RpgNet.Allocate(n.id));
                } else if (data.isAllocated(n.id) && n.active != null) {
                    selected = selected == n.active ? null : n.active;
                }
                return;
            }
        }
        selected = null;
    }

    private static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.fill(0, 0, Math.round(length), 1, color);
        g.pose().popPose();
    }
}
