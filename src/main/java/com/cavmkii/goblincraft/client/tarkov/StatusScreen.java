package com.cavmkii.goblincraft.client.tarkov;

import com.cavmkii.goblincraft.tarkov.Body;
import com.cavmkii.goblincraft.tarkov.BodyPart;
import com.cavmkii.goblincraft.tarkov.MedItem;
import com.cavmkii.goblincraft.tarkov.TarkovNet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** STATUS: the body, each part's health and conditions, and the meds you carry. Click a med, then a part. */
public class StatusScreen extends Screen {
    private int selectedSlot = -1;

    public StatusScreen() {
        super(Component.literal("Status"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    /** Body part rectangles: x, y, w, h in units, relative to the figure's top-left. */
    private static final int[][] LAYOUT = {
            {3, 0, 2, 2},    // head
            {2, 2, 4, 3},    // thorax
            {2, 5, 4, 2},    // stomach
            {0, 2, 2, 5},    // left arm (drawn on the viewer's left)
            {6, 2, 2, 5},    // right arm
            {2, 7, 2, 5},    // left leg
            {4, 7, 2, 5},    // right leg
    };

    private int unit() {
        return Math.max(8, Math.min(16, (height - 60) / 13));
    }

    private int figX() {
        return width / 2 - 8 * unit() - 20;
    }

    private int figY() {
        return 34;
    }

    private int[] rect(BodyPart p) {
        int u = unit();
        int[] l = LAYOUT[p.ordinal()];
        return new int[] {figX() + l[0] * u, figY() + l[1] * u, l[2] * u, l[3] * u};
    }

    private List<Integer> medSlots() {
        List<Integer> out = new ArrayList<>();
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof MedItem) {
                out.add(i);
            }
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE0101014);
        TarkovNet.BodySync body = ClientTarkov.body;
        float max = minecraft.player.getMaxHealth();
        g.drawString(font, "STATUS", 10, 10, 0xFFFFE080, true);
        g.drawString(font, String.format("Health %.0f / %.0f", minecraft.player.getHealth(), max), 70, 10, 0xFFCCCCCC, false);
        if (body.painkillerTicks() > 0) {
            g.drawString(font, "Painkillers: " + body.painkillerTicks() / 20 + "s", 190, 10, 0xFF55FFFF, false);
        }

        BodyPart hovered = null;
        for (BodyPart p : BodyPart.values()) {
            int[] r = rect(p);
            float frac = body.body()[p.ordinal()];
            boolean destroyed = frac <= 0.0001f;
            int color = destroyed ? 0xFF202020 : lerpColor(0xFFB03030, 0xFF3FA34D, frac);
            boolean hover = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            if (hover) {
                hovered = p;
            }
            g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, hover && selectedSlot >= 0 ? 0xFF55FFFF : 0xFF000000);
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], color);
            int c = body.conditions()[p.ordinal()];
            StringBuilder tags = new StringBuilder();
            if ((c & Body.HEAVY_BLEED) != 0) tags.append("HB ");
            else if ((c & Body.LIGHT_BLEED) != 0) tags.append("B ");
            if ((c & Body.FRACTURE) != 0) tags.append("F");
            if (tags.length() > 0) {
                g.drawString(font, tags.toString().trim(), r[0] + 2, r[1] + 2, 0xFFFFFF55, true);
            }
        }

        // Part list on the right of the figure.
        int lx = figX() + 8 * unit() + 16;
        int ly = figY();
        for (BodyPart p : BodyPart.values()) {
            float frac = body.body()[p.ordinal()];
            String state = frac <= 0.0001f ? "DESTROYED" : String.format("%.0f / %.0f", frac * p.share * max, p.share * max);
            g.drawString(font, p.label, lx, ly, 0xFFFFFFFF, false);
            g.drawString(font, state, lx + 62, ly, frac <= 0.0001f ? 0xFFFF5555 : 0xFFAAAAAA, false);
            int c = body.conditions()[p.ordinal()];
            List<String> conds = new ArrayList<>();
            if ((c & Body.HEAVY_BLEED) != 0) conds.add("heavy bleed");
            if ((c & Body.LIGHT_BLEED) != 0) conds.add("light bleed");
            if ((c & Body.FRACTURE) != 0) conds.add("fracture");
            if (!conds.isEmpty()) {
                g.drawString(font, String.join(", ", conds), lx + 8, ly + 9, 0xFFFF7755, false);
                ly += 9;
            }
            ly += 12;
        }

        // Meds carried.
        ly += 6;
        g.drawString(font, selectedSlot >= 0 ? "Now click a body part" : "Meds (click one, then a part)", lx, ly, 0xFFFFE080, false);
        ly += 12;
        int mx = lx;
        String medTip = null;
        for (int slot : medSlots()) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            boolean sel = slot == selectedSlot;
            g.fill(mx, ly, mx + 20, ly + 20, sel ? 0xFF55FFFF : 0xFF2A2A30);
            g.fill(mx + 1, ly + 1, mx + 19, ly + 19, 0xFF3C3C44);
            g.renderItem(stack, mx + 2, ly + 2);
            g.renderItemDecorations(font, stack, mx + 2, ly + 2);
            if (mouseX >= mx && mouseX < mx + 20 && mouseY >= ly && mouseY < ly + 20) {
                medTip = stack.getHoverName().getString() + ": " + ((MedItem) stack.getItem()).med.describe();
            }
            mx += 22;
        }
        if (medSlots().isEmpty()) {
            g.drawString(font, "none - make them at a Hideout Bench with a Medstation", lx, ly + 4, 0xFF777777, false);
        }

        if (medTip != null) {
            g.renderTooltip(font, List.of(Component.literal(medTip)), Optional.empty(), mouseX, mouseY);
        } else if (hovered != null) {
            float frac = body.body()[hovered.ordinal()];
            g.renderTooltip(font, List.of(Component.literal(hovered.label).withStyle(ChatFormatting.WHITE),
                    Component.literal(frac <= 0.0001f ? "Destroyed: needs a surgical kit" : (int) (frac * 100) + "%").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private int medsTop() {
        int ly = figY();
        for (BodyPart p : BodyPart.values()) {
            ly += 12 + (ClientTarkov.body.conditions()[p.ordinal()] != 0 ? 9 : 0);
        }
        return ly + 18;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int lx = figX() + 8 * unit() + 16;
        int ly = medsTop();
        int mx = lx;
        for (int slot : medSlots()) {
            if (mouseX >= mx && mouseX < mx + 20 && mouseY >= ly && mouseY < ly + 20) {
                selectedSlot = selectedSlot == slot ? -1 : slot;
                return true;
            }
            mx += 22;
        }
        if (selectedSlot >= 0) {
            for (BodyPart p : BodyPart.values()) {
                int[] r = rect(p);
                if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                    ClientPlayNetworking.send(new TarkovNet.TreatPart(selectedSlot, p.ordinal()));
                    selectedSlot = -1;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientTarkov.OPEN_STATUS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int gr = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }
}
