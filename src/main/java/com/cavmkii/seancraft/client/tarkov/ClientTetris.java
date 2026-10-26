package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tetris.GridId;
import com.cavmkii.seancraft.tetris.InvData;
import com.cavmkii.seancraft.tetris.Placed;
import com.cavmkii.seancraft.tetris.Tetris;
import com.cavmkii.seancraft.tetris.TetrisNet;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.item.ItemStack;

/** Client side of the Tetris inventory: the last sync, and E opening the Tarkov screen. */
public final class ClientTetris {
    public static TetrisNet.Sync sync = new TetrisNet.Sync(ItemStack.EMPTY, ItemStack.EMPTY, List.of(), List.of(), List.of(), List.of(),
            2, 2, List.of(), false, List.of());
    public static List<TetrisNet.CraftEntry> crafts = List.of();

    private ClientTetris() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TetrisNet.Sync.TYPE, (payload, ctx) -> {
            sync = payload;
            InvData view = new InvData();
            view.rig = payload.rig();
            view.backpack = payload.backpack();
            view.pockets.addAll(payload.pockets());
            view.rigItems.addAll(payload.rigItems());
            view.backpackItems.addAll(payload.backpackItems());
            view.pouch.addAll(payload.pouch());
            view.unsorted.addAll(payload.unsorted());
            Tetris.clientView = view;
        });
        ClientPlayNetworking.registerGlobalReceiver(TetrisNet.CraftList.TYPE, (payload, ctx) -> crafts = payload.entries());
        ClientPlayNetworking.registerGlobalReceiver(TetrisNet.Open.TYPE, (payload, ctx) -> {
            Minecraft mc = ctx.client();
            if (payload.tab() < 0) {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            } else if (mc.screen instanceof TarkovScreen screen) {
                screen.setTab(payload.tab());
            } else if (mc.screen == null) {
                mc.setScreen(new TarkovScreen(payload.tab()));
            }
        });
        // Before vanilla handles keys: in survival, E opens the Tarkov screen. Riding something with its own
        // inventory (horse, chest boat) keeps the vanilla behaviour.
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.screen != null || !Tetris.active(mc.player)
                    || mc.player.getVehicle() instanceof HasCustomInventoryScreen) {
                return;
            }
            boolean open = false;
            while (mc.options.keyInventory.consumeClick()) {
                open = true;
            }
            if (open) {
                open(TarkovScreen.GEAR);
            }
        });
    }

    public static void open(int tab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            mc.setScreen(new TarkovScreen(tab));
        }
    }

    /** Same order as {@link Tetris#carried}: main inventory, off-hand, pockets, rig, backpack, pouch. */
    public static List<ItemStack> carried() {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> out = new ArrayList<>(mc.player.getInventory().items);
        out.add(mc.player.getOffhandItem());
        InvData view = Tetris.clientView;
        for (GridId id : GridId.values()) {
            for (Placed p : view.list(id)) {
                out.add(p.stack);
            }
        }
        return out;
    }
}
