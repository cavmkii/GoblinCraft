package com.cavmkii.goblincraft.client.tarkov;

import com.cavmkii.goblincraft.tarkov.TarkovNet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Bottom left: HIDEOUT, or RAID with the timer, threat level and when it rises next. */
final class RaidHud {
    private RaidHud() {}

    /** Injuries listed above the raid readout, newest-looking first. */
    private static void injuries(GuiGraphics g, Minecraft mc, int x, int bottom) {
        TarkovNet.BodySync body = ClientTarkov.body;
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (com.cavmkii.goblincraft.tarkov.BodyPart p : com.cavmkii.goblincraft.tarkov.BodyPart.values()) {
            int c = body.conditions()[p.ordinal()];
            if (body.body()[p.ordinal()] <= 0.0001f) lines.add("Destroyed - " + p.label);
            if ((c & com.cavmkii.goblincraft.tarkov.Body.HEAVY_BLEED) != 0) lines.add("Heavy bleed - " + p.label);
            if ((c & com.cavmkii.goblincraft.tarkov.Body.LIGHT_BLEED) != 0) lines.add("Light bleed - " + p.label);
            if ((c & com.cavmkii.goblincraft.tarkov.Body.FRACTURE) != 0) lines.add("Fracture - " + p.label);
        }
        if (body.painkillerTicks() > 0) {
            lines.add("Painkillers " + body.painkillerTicks() / 20 + "s");
        }
        int y = bottom - lines.size() * 10;
        for (String line : lines) {
            g.drawString(mc.font, line, x, y, line.startsWith("Painkillers") ? 0xFF55FFFF : 0xFFFF7755, true);
            y += 10;
        }
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        TarkovNet.RaidState s = ClientTarkov.raid;
        if (mc.player == null || mc.options.hideGui || s.state() == 0) {
            return;
        }
        int x = 4;
        int y = g.guiHeight() - 30;
        injuries(g, mc, x, y - 4);
        if (s.state() == 1) {
            g.drawString(mc.font, "HIDEOUT", x, y, 0xFF55FF55, true);
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
