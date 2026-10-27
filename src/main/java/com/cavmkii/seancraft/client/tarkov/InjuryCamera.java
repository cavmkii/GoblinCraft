package com.cavmkii.seancraft.client.tarkov;

import com.cavmkii.seancraft.tarkov.BodyPart;
import com.cavmkii.seancraft.tarkov.Injury;
import com.cavmkii.seancraft.tetris.Tetris;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;

/**
 * What injuries do to your view. Bad legs make every other step land hard (a dip and a roll toward the bad side),
 * hurt arms make a drawn bow wander, and any injury or bleeding adds a slow sway while you stand still. Everything
 * scales with severity, and painkillers calm it down the same way they calm the penalties.
 */
public final class InjuryCamera {
    private InjuryCamera() {}

    private static Injury injury(LocalPlayer player) {
        return player == null || !Tetris.active(player) ? Injury.NONE : ClientTarkov.body.injury();
    }

    /** Full-severity legs with no painkillers: can't sprint at all. */
    public static boolean noSprint() {
        return injury(Minecraft.getInstance().player).noSprint();
    }

    public static void apply(PoseStack pose, float partialTick) {
        LocalPlayer p = Minecraft.getInstance().player;
        Injury inj = injury(p);
        if (!inj.any()) {
            return;
        }
        float t = p.tickCount + partialTick;
        float limp = inj.limp();

        // Uneven steps: the walk cycle is two steps; the bad leg's step dips and rolls.
        float walk = -(p.walkDist + (p.walkDist - p.walkDistO) * partialTick);
        float bob = Mth.lerp(partialTick, p.oBob, p.bob);
        if (limp > 0.01f && bob > 0.001f) {
            var body = ClientTarkov.body;
            float left = Injury.limb(body.body(), body.fracture(), body.scar(), BodyPart.LEFT_LEG);
            float right = Injury.limb(body.body(), body.fracture(), body.scar(), BodyPart.RIGHT_LEG);
            float side = left >= right ? -1 : 1;
            float step = Math.max(0, Mth.sin(walk * Mth.PI) * side);
            float hard = step * step * bob * limp;
            pose.translate(0, hard * 0.9f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(side * hard * 45));
            pose.mulPose(Axis.XP.rotationDegrees(hard * 30));
        }

        // Standing sway from bad legs, bad arms or blood loss.
        float arms = inj.pain() ? inj.arms() * 0.5f : inj.arms();
        float sway = Math.min(1, limp * 0.6f + arms * 0.3f + inj.bleed() * 0.7f);
        if (sway > 0.01f) {
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.035f) * 0.9f * sway));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.05f + 1.3f) * 0.5f * sway));
        }

        // A drawn bow or trident won't hold still with hurt arms.
        if (arms > 0.01f && p.isUsingItem()
                && (p.getUseItem().getItem() instanceof BowItem || p.getUseItem().getItem() instanceof CrossbowItem
                || p.getUseItem().getItem() instanceof TridentItem)) {
            float shake = arms * 1.6f;
            pose.mulPose(Axis.YP.rotationDegrees((Mth.sin(t * 0.31f) + 0.6f * Mth.sin(t * 0.83f)) * shake));
            pose.mulPose(Axis.XP.rotationDegrees((Mth.sin(t * 0.27f + 2) + 0.5f * Mth.sin(t * 0.71f)) * shake));
        }
    }
}
