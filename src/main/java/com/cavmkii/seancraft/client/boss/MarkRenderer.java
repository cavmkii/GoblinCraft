package com.cavmkii.seancraft.client.boss;

import com.cavmkii.seancraft.SeanCraft;
import com.cavmkii.seancraft.boss.BossEntity;
import com.cavmkii.seancraft.boss.Mark;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Mark on the vanilla humanoid model until the shared boss rig (design doc B.2) exists. Wind-ups raise his arm and the
 * strike brings it down, so tells read even without custom animations.
 */
public class MarkRenderer extends HumanoidMobRenderer<Mark, MarkRenderer.Model> {
    private static final ResourceLocation TEXTURE = SeanCraft.id("textures/entity/mark.png");

    public MarkRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Mark entity) {
        return TEXTURE;
    }

    public static class Model extends HumanoidModel<Mark> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(Mark mark, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
            super.setupAnim(mark, limbSwing, limbSwingAmount, age, yaw, pitch);
            BossEntity.State state = mark.state();
            if (state == BossEntity.State.WINDUP) {
                rightArm.xRot = -2.6f + (float) Math.sin(age * 0.8f) * 0.08f;
                rightArm.zRot = -0.2f;
            } else if (state == BossEntity.State.ACTIVE) {
                rightArm.xRot = -0.4f;
            } else if (state == BossEntity.State.TRANSITION) {
                rightArm.xRot = -1.2f;
                leftArm.xRot = -1.2f;
                head.xRot = 0.5f;
            }
        }
    }
}
