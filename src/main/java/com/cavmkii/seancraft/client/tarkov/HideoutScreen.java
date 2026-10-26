package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tarkov.TarkovNet;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The Hideout Bench: MODULES to build and upgrade, CRAFT for hideout-only items. Materials come from you and the stash. */
public class HideoutScreen extends Screen {
    private static final int ROW = 46;

    final BlockPos pos;
    private TarkovNet.HideoutView view;
    private int tab;
    private double scroll;
    private int ticks;

    HideoutScreen(TarkovNet.HideoutView view) {
        super(Component.literal("Hideout"));
        this.pos = view.pos();
        this.view = view;
    }

    void update(TarkovNet.HideoutView view) {
        this.view = view;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Refresh once a second for build progress and stash counts.
        if (++ticks % 20 == 0) {
            ClientPlayNetworking.send(new TarkovNet.HideoutAction(pos, -1, ""));
        }
    }

    private int panelWidth() {
        return Math.min(420, width - 16);
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private int listTop() {
        return 44;
    }

    private int listBottom() {
        return height - 8;
    }

    private List<TarkovNet.Entry> shown() {
        List<TarkovNet.Entry> out = new ArrayList<>();
        for (TarkovNet.Entry e : view.entries()) {
            if (e.kind() == tab) {
                out.add(e);
            }
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xE0101014);
        int x0 = left();
        int w = panelWidth();
        g.drawString(font, "HIDEOUT", x0, 8, 0xFFFFE080, true);
        g.drawString(font, view.summary(), x0 + 60, 8, 0xFFAAAAAA, false);
        String[] tabs = {"MODULES", "CRAFT"};
        for (int t = 0; t < 2; t++) {
            int tx = x0 + t * 80;
            boolean active = t == tab;
            g.fill(tx, 22, tx + 76, 38, active ? 0xFF3A3A44 : 0xFF202026);
            g.drawString(font, tabs[t], tx + (76 - font.width(tabs[t])) / 2, 26, active ? 0xFFFFFFFF : 0xFF888888, false);
        }

        List<TarkovNet.Entry> entries = shown();
        int top = listTop();
        int bottom = listBottom();
        double maxScroll = Math.max(0, entries.size() * ROW - (bottom - top));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.enableScissor(x0, top, x0 + w, bottom);
        for (int i = 0; i < entries.size(); i++) {
            int y = top + i * ROW - (int) scroll;
            if (y + ROW < top || y > bottom) {
                continue;
            }
            drawEntry(g, entries.get(i), x0, y, w, mouseX, mouseY);
        }
        g.disableScissor();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No vanilla blur; render() fills its own backdrop.
    }

    private void drawEntry(GuiGraphics g, TarkovNet.Entry e, int x, int y, int w, int mouseX, int mouseY) {
        g.fill(x, y, x + w, y + ROW - 3, 0xFF1C1C22);
        g.renderItem(new ItemStack(item(e.icon())), x + 4, y + 4);
        g.drawString(font, e.title(), x + 24, y + 4, 0xFFFFFFFF, false);

        g.pose().pushPose();
        g.pose().translate(x + 24, y + 15, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, font.plainSubstrByWidth(e.subtitle(), (int) ((w - 110) / 0.75)), 0, 0, 0xFFAAAAAA, false);
        g.pose().popPose();

        // Costs: icon and have/need, red when short.
        int cx = x + 24;
        for (TarkovNet.CostLine c : e.costs()) {
            g.pose().pushPose();
            g.pose().translate(cx, y + 25, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.renderItem(new ItemStack(item(c.icon())), 0, 0);
            g.pose().popPose();
            String text = Math.min(c.have(), 9999) + "/" + c.need();
            g.drawString(font, text, cx + 14, y + 29, c.have() >= c.need() ? 0xFF7CFC7C : 0xFFFF6666, false);
            cx += 18 + font.width(text) + 6;
        }

        // Button / status on the right.
        int bx = x + w - 84;
        int by = y + 6;
        boolean ready = e.status() == TarkovNet.Entry.READY;
        boolean hover = mouseX >= bx && mouseX < bx + 78 && mouseY >= by && mouseY < by + 18;
        int bg = ready ? (hover ? 0xFF4C8C4C : 0xFF3A6A3A) : 0xFF2A2A30;
        g.fill(bx, by, bx + 78, by + 18, bg);
        String label = switch (e.status()) {
            case TarkovNet.Entry.READY -> e.kind() == TarkovNet.Entry.MODULE ? "BUILD" : "CRAFT";
            case TarkovNet.Entry.BUILDING -> e.progress() + "%";
            case TarkovNet.Entry.MAXED -> "MAX";
            default -> "LOCKED";
        };
        g.drawString(font, label, bx + (78 - font.width(label)) / 2, by + 5, ready ? 0xFFFFFFFF : 0xFF888888, false);
        if (e.status() == TarkovNet.Entry.BUILDING) {
            g.fill(bx, by + 18, bx + 78 * e.progress() / 100, by + 20, 0xFFE0C060);
        }
        g.pose().pushPose();
        g.pose().translate(bx, by + 24, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, font.plainSubstrByWidth(e.note(), (int) (100 / 0.75)), 0, 0,
                ready ? 0xFF7CFC7C : e.status() == TarkovNet.Entry.MAXED ? 0xFFE0C060 : 0xFFFF9966, false);
        g.pose().popPose();
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null ? BuiltInRegistries.ITEM.get(key) : net.minecraft.world.item.Items.BARRIER;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x0 = left();
        for (int t = 0; t < 2; t++) {
            int tx = x0 + t * 80;
            if (mouseX >= tx && mouseX < tx + 76 && mouseY >= 22 && mouseY < 38) {
                tab = t;
                scroll = 0;
                return true;
            }
        }
        if (mouseY >= listTop() && mouseY < listBottom()) {
            List<TarkovNet.Entry> entries = shown();
            int i = (int) ((mouseY - listTop() + scroll) / ROW);
            if (i >= 0 && i < entries.size()) {
                TarkovNet.Entry e = entries.get(i);
                int y = listTop() + i * ROW - (int) scroll;
                int bx = x0 + panelWidth() - 84;
                if (e.status() == TarkovNet.Entry.READY && mouseX >= bx && mouseX < bx + 78 && mouseY >= y + 6 && mouseY < y + 24) {
                    ClientPlayNetworking.send(new TarkovNet.HideoutAction(pos, e.kind(), e.id()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * 20;
        return true;
    }
}
