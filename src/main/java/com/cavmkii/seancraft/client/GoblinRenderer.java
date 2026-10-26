package com.cavmkii.seancraft.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Joe and the babies: a green piglin. Size comes from the entity's scale (and baby flag). */
public class GoblinRenderer<T extends Mob> extends HumanoidMobRenderer<T, TintedPiglinModel<T>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/piglin/piglin.png");

    public GoblinRenderer(EntityRendererProvider.Context context, int tint) {
        super(context, new TintedPiglinModel<>(context.bakeLayer(ModelLayers.PIGLIN), tint), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
