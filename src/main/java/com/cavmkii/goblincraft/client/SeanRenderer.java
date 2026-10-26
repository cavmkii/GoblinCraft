package com.cavmkii.goblincraft.client;

import com.cavmkii.goblincraft.entity.Sean;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Sean wears the wandering trader's robes. */
public class SeanRenderer extends MobRenderer<Sean, VillagerModel<Sean>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/wandering_trader.png");

    public SeanRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Sean entity) {
        return TEXTURE;
    }
}
