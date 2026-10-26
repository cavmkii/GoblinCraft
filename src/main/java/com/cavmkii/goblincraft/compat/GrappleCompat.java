package com.cavmkii.goblincraft.compat;

import com.cavmkii.goblincraft.rpg.Rpg;
import com.cavmkii.goblincraft.rpg.Skill;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;

/**
 * Grappling Hook Mod (yyon, mod id {@code grapplemod}) support, with no compile-time dependency: firing a
 * grappling hook trains Bows & Crossbows, and its hooks and launchers go to the hotbar on pickup.
 */
public final class GrappleCompat {
    public static final String MOD_ID = "grapplemod";
    private static final double XP_PER_SHOT = 2;
    private static final long COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private GrappleCompat() {}

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Projectile) || !MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace())) {
                return;
            }
            // The hook keeps its shooter in its own field, not the vanilla owner, but it spawns at the shooter's eyes.
            ServerPlayer player = null;
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(entity) < 4 && (isGrappleItem(p.getMainHandItem().getItem()) || isGrappleItem(p.getOffhandItem().getItem()))) {
                    player = p;
                    break;
                }
            }
            if (player == null) {
                return;
            }
            // A double hook fires two at once; count it as one shot.
            long now = level.getGameTime();
            Long last = LAST.get(player.getUUID());
            if (last == null || now - last >= COOLDOWN_TICKS) {
                LAST.put(player.getUUID(), now);
                Rpg.addSkillXp(player, Skill.BOWS, XP_PER_SHOT);
            }
        });
    }

    public static boolean isGrappleItem(Item item) {
        return MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }
}
