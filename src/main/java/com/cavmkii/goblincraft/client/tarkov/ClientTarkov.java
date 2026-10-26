package com.cavmkii.goblincraft.client.tarkov;

import com.cavmkii.goblincraft.tarkov.TarkovContent;
import com.cavmkii.goblincraft.tarkov.TarkovNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import org.lwjgl.glfw.GLFW;

/** Client side of the raid loop: raid HUD, Hideout screen, secure container key. */
public final class ClientTarkov {
    public static TarkovNet.RaidState raid = new TarkovNet.RaidState(0, 0, 0, -1);

    public static final KeyMapping OPEN_SECURE = new KeyMapping("key.goblincraft.secure", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
            "key.categories.goblincraft");

    private ClientTarkov() {}

    public static void init() {
        KeyBindingHelper.registerKeyBinding(OPEN_SECURE);
        MenuScreens.register(TarkovContent.SECURE_MENU, SecureScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(TarkovNet.RaidState.TYPE, (payload, ctx) -> raid = payload);
        ClientPlayNetworking.registerGlobalReceiver(TarkovNet.HideoutView.TYPE, (payload, ctx) -> {
            Minecraft mc = ctx.client();
            if (mc.screen instanceof HideoutScreen screen && screen.pos.equals(payload.pos())) {
                screen.update(payload);
            } else if (mc.screen == null) {
                mc.setScreen(new HideoutScreen(payload));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_SECURE.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ClientPlayNetworking.send(new TarkovNet.OpenSecure());
                }
            }
        });
        HudRenderCallback.EVENT.register(RaidHud::render);
    }
}
