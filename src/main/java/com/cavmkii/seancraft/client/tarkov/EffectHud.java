package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tarkov.Body;
import com.cavmkii.seancraft.tarkov.BodyPart;
import com.cavmkii.seancraft.tarkov.Med;
import com.cavmkii.seancraft.tarkov.TarkovContent;
import com.cavmkii.seancraft.tarkov.TarkovNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bottom left, above the raid readout: injuries (with what fixes them) and timed effects as small icons with the
 * time left. Stands in for the vanilla effect icons, which show no time.
 */
final class EffectHud {
    private static final int ICON = 12;

    private EffectHud() {}

    private record Injury(ItemStack icon, String text, String cure, int color) {}

    private static List<Injury> injuries() {
        TarkovNet.BodySync body = ClientTarkov.body;
        List<Injury> out = new ArrayList<>();
        for (BodyPart p : BodyPart.values()) {
            int c = body.conditions()[p.ordinal()];
            if (body.body()[p.ordinal()] <= 0.0001f) {
                out.add(new Injury(new ItemStack(Items.BARRIER), "Destroyed - " + p.label, "surgical kit", 0xFFFF5555));
            }
            if ((c & Body.HEAVY_BLEED) != 0) {
                out.add(new Injury(new ItemStack(Items.RED_DYE), "Heavy bleed - " + p.label, "tourniquet", 0xFFFF5555));
            }
            if ((c & Body.LIGHT_BLEED) != 0) {
                out.add(new Injury(new ItemStack(Items.RED_DYE), "Light bleed - " + p.label, "bandage", 0xFFFF9966));
            }
            if ((c & Body.FRACTURE) != 0) {
                out.add(new Injury(new ItemStack(Items.BONE), "Fracture - " + p.label, "splint", 0xFFFFAA55));
            }
        }
        return out;
    }

    private record Chip(TextureAtlasSprite sprite, ItemStack item, int ticks, boolean bad) {}

    private static List<Chip> chips(Minecraft mc) {
        List<Chip> out = new ArrayList<>();
        if (ClientTarkov.body.painkillerTicks() > 0) {
            out.add(new Chip(null, new ItemStack(TarkovContent.MEDS.get(Med.PAINKILLERS)), ClientTarkov.body.painkillerTicks(), false));
        }
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            if (e.showIcon()) {
                out.add(new Chip(mc.getMobEffectTextures().get(e.getEffect()), ItemStack.EMPTY, e.isInfiniteDuration() ? -1 : e.getDuration(),
                        e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL));
            }
        }
        return out;
    }

    private static String time(int ticks) {
        if (ticks < 0) {
            return "∞";
        }
        int s = (ticks + 19) / 20;
        return s >= 60 ? s / 60 + ":" + String.format("%02d", s % 60) : s + "s";
    }

    /** Draws upward from {@code bottom}; returns the new top. */
    static int render(GuiGraphics g, Minecraft mc, int x, int bottom) {
        int y = bottom;
        List<Injury> injuries = injuries();
        y -= injuries.size() * 10;
        int line = y;
        for (Injury i : injuries) {
            g.pose().pushPose();
            g.pose().translate(x, line, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            g.renderItem(i.icon, 0, 0);
            g.pose().popPose();
            g.drawString(mc.font, i.text, x + 10, line, i.color, true);
            // What fixes it, small and grey, if it fits left of the hotbar.
            int cx = x + 12 + mc.font.width(i.text);
            if (cx + mc.font.width(i.cure) * 0.75f < g.guiWidth() / 2f - 96) {
                g.pose().pushPose();
                g.pose().translate(cx, line + 2, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.drawString(mc.font, i.cure, 0, 0, 0xFF9A9AA4, true);
                g.pose().popPose();
            }
            line += 10;
        }

        List<Chip> chips = chips(mc);
        if (chips.isEmpty()) {
            return y;
        }
        // One row of chips, wrapping before it gets in the way.
        int maxW = Math.min(200, g.guiWidth() / 2 - 100);
        List<List<Chip>> rows = new ArrayList<>();
        List<Chip> row = new ArrayList<>();
        int w = 0;
        for (Chip c : chips) {
            int cw = ICON + 2 + mc.font.width(time(c.ticks)) + 6;
            if (!row.isEmpty() && w + cw > maxW) {
                rows.add(row);
                row = new ArrayList<>();
                w = 0;
            }
            row.add(c);
            w += cw;
        }
        rows.add(row);
        y -= rows.size() * (ICON + 2);
        int ry = y;
        for (List<Chip> r : rows) {
            int cx = x;
            for (Chip c : r) {
                String t = time(c.ticks);
                // Blink for the last few seconds so you notice it running out.
                boolean ending = c.ticks >= 0 && c.ticks < 100;
                boolean dim = ending && (c.ticks / 5) % 2 == 0;
                int cw = ICON + 2 + mc.font.width(t) + 2;
                g.fill(cx - 1, ry - 1, cx + cw, ry + ICON + 1, 0x60000000);
                if (c.sprite != null) {
                    g.setColor(1, 1, 1, dim ? 0.4f : 1f);
                    g.blit(cx, ry, 0, ICON, ICON, c.sprite);
                    g.setColor(1, 1, 1, 1);
                } else {
                    g.pose().pushPose();
                    g.pose().translate(cx, ry, 0);
                    g.pose().scale(ICON / 16f, ICON / 16f, 1);
                    g.renderItem(c.item, 0, 0);
                    g.pose().popPose();
                }
                int color = ending ? 0xFFFFE060 : c.bad ? 0xFFFF8877 : 0xFFDDDDDD;
                g.drawString(mc.font, t, cx + ICON + 2, ry + 2, color, true);
                cx += cw + 4;
            }
            ry += ICON + 2;
        }
        return y;
    }
}
