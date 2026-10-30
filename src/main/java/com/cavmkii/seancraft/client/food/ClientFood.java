package com.cavmkii.seancraft.client.food;

import com.cavmkii.seancraft.food.Food;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The meals the server says are active, for the HUD and the stamina bar. */
public final class ClientFood {
    public static List<Food.Meal> meals = List.of();

    private ClientFood() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(Food.Sync.TYPE, (payload, ctx) -> meals = payload.meals());
    }

    public static float stamina() {
        return Food.stamina(meals);
    }
}
