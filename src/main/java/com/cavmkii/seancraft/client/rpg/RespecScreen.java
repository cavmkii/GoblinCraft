package com.cavmkii.seancraft.client.rpg;

import com.cavmkii.seancraft.rpg.RpgNet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** The Altar of Regret's warning. Confirming clears your web and kills you. */
final class RespecScreen {
    private RespecScreen() {}

    static void open(RpgNet.OpenRespec info) {
        Minecraft mc = Minecraft.getInstance();
        int refunded = (int) Math.round(info.spent() * info.refundPercent() / 100.0);
        String orb = info.needsOrb() ? (info.hasOrb() ? "\nIt will also consume an Orb of Regret." : "\nAt your level it demands an Orb of Regret, and you have none.") : "";
        Component title = Component.literal("Altar of Regret");
        Component body = Component.literal("Clear your passive web? You will DIE.\nYou get back " + refunded + " of your " + info.spent()
                + " points (" + info.refundPercent() + "%). The rest are lost forever." + orb);
        mc.setScreen(new ConfirmScreen(yes -> {
            mc.setScreen(null);
            if (yes) {
                ClientPlayNetworking.send(new RpgNet.Respec(info.pos()));
            }
        }, title, body, Component.literal("Pay the price"), Component.literal("Walk away")));
    }
}
