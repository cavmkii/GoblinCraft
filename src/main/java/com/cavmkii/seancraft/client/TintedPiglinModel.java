package com.cavmkii.seancraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;

/** The vanilla piglin body, tinted goblin green. */
public class TintedPiglinModel<T extends Mob> extends PiglinModel<T> {
    private final int tint;

    public TintedPiglinModel(ModelPart root, int tint) {
        super(root);
        this.tint = tint;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, multiply(color, tint));
    }

    private static int multiply(int a, int b) {
        int alpha = ((a >>> 24) * (b >>> 24)) / 255;
        int red = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF)) / 255;
        int green = (((a >> 8) & 0xFF) * ((b >> 8) & 0xFF)) / 255;
        int blue = ((a & 0xFF) * (b & 0xFF)) / 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
