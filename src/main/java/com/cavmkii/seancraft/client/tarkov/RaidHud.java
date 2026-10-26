package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tarkov.TarkovNet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Bottom left: HIDEOUT, or RAID with the timer, threat level and when it rises next; injuries and effects above. */
final class RaidHud {
    private RaidHud() {}

    private static final long BANNER_MS = 4000;

    /** Raid banners sit at the top of the screen and fade in and out. */
    private static void banner(GuiGraphics g, Minecraft mc) {
        TarkovNet.Banner b = ClientTarkov.banner;
        if (b == null) {
            return;
        }
        long age = net.minecraft.Util.getMillis() - ClientTarkov.bannerAt;
        if (age > BANNER_MS) {
            return;
        }
        float alpha = Math.min(1f, Math.min(age / 300f, (BANNER_MS - age) / 600f));
        int a = Math.max(8, (int) (alpha * 255)) << 24;
        int cx = g.guiWidth() / 2;
        int y = 6;
        int w = Math.max((int) (mc.font.width(b.title()) * 1.5f), mc.font.width(b.subtitle())) + 16;
        g.fill(cx - w / 2, y - 3, cx + w / 2, y + 26, (int) (alpha * 0x90) << 24);
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(1.5f, 1.5f, 1);
        g.drawString(mc.font, b.title(), -mc.font.width(b.title()) / 2, 0, a | (b.color() & 0xFFFFFF), true);
        g.pose().popPose();
        g.drawString(mc.font, b.subtitle(), cx - mc.font.width(b.subtitle()) / 2, y + 15, a | 0xDDDDDD, true);
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.options.hideGui) {
            banner(g, mc);
        }
        TarkovNet.RaidState s = ClientTarkov.raid;
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        int x = 4;
        int y = g.guiHeight() - 30;
        EffectHud.render(g, mc, x, s.state() == 0 ? g.guiHeight() - 4 : y - 4);
        if (s.state() == 0) {
            return;
        }
        if (s.state() == 1) {
            g.drawString(mc.font, "HIDEOUT", x, y, 0xFF55FF55, true);
            return;
        }
        if (s.state() == 3) {
            g.drawString(mc.font, "SPAWN (safe)", x, y, 0xFF55FFFF, true);
            return;
        }
        String time = String.format("%02d:%02d", s.raidSeconds() / 60, s.raidSeconds() % 60);
        g.drawString(mc.font, "RAID " + time, x, y, 0xFFFF5555, true);
        StringBuilder threat = new StringBuilder("Threat ");
        for (int i = 1; i <= 4; i++) {
            threat.append(i <= s.threat() ? '■' : '□');
        }
        if (s.nextThreat() >= 0) {
            threat.append(String.format("  next %d:%02d", s.nextThreat() / 60, s.nextThreat() % 60));
        }
        int color = s.threat() == 0 ? 0xFFAAAAAA : s.threat() < 3 ? 0xFFFFAA00 : 0xFFFF5555;
        g.drawString(mc.font, threat.toString(), x, y + 10, color, true);
    }
}
