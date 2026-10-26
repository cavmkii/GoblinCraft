package com.cavmkii.goblincraft.client;

import com.cavmkii.goblincraft.entity.ModEntities;
import com.cavmkii.goblincraft.client.rpg.ClientRpg;
import com.cavmkii.goblincraft.client.tarkov.ClientTarkov;
import com.cavmkii.goblincraft.item.ModItems;
import com.cavmkii.goblincraft.rpg.RpgContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.DyedItemColor;

public final class GoblinCraftClient implements ClientModInitializer {
    private static final int JOE_GREEN = 0xFF8FD46A;
    private static final int BABY_GREEN = 0xFFB4E68C;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.JOE, ctx -> new GoblinRenderer<>(ctx, JOE_GREEN));
        EntityRendererRegistry.register(ModEntities.BABY_COMMANDO, ctx -> new GoblinRenderer<>(ctx, BABY_GREEN));
        EntityRendererRegistry.register(ModEntities.BABY_GRENADIER, ctx -> new GoblinRenderer<>(ctx, BABY_GREEN));
        EntityRendererRegistry.register(ModEntities.BABY_BERSERKER, ctx -> new GoblinRenderer<>(ctx, JOE_GREEN));
        EntityRendererRegistry.register(ModEntities.SEAN, SeanRenderer::new);
        EntityRendererRegistry.register(ModEntities.SEAN_SECURITY, SecurityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPELL, ctx -> new ThrownItemRenderer<>(ctx, 1.75f, true));
        EntityRendererRegistry.register(ModEntities.BABY_BOTTLE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(RpgContent.SKILL_PROJECTILE, ctx -> new ThrownItemRenderer<>(ctx, 1.25f, true));
        ClientRpg.init();
        ClientTarkov.init();

        // Merch and the bonnet are dyed leather; tint the base layer of their icons.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex > 0 ? -1 : DyedItemColor.getOrDefault(stack, -1),
                ModItems.MERCH, ModItems.BABY_BONNET);

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (ModItems.DESCRIBED.contains(stack.getItem()) || stack.is(RpgContent.ORB_OF_REGRET) || stack.is(RpgContent.ALTAR_ITEM)
                    || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("goblincraft")
                    && net.minecraft.client.resources.language.I18n.exists("item.goblincraft." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ".desc")) {
                String key = "item.goblincraft." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + ".desc";
                lines.add(1, Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
