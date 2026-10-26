package com.cavmkii.goblincraft.rpg;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Small entry points for the mixins. */
public final class RpgHooks {
    /** The entity currently drinking a potion, if any (set by PotionItemMixin). */
    public static LivingEntity drinking;

    private RpgHooks() {}

    public static void onEnchanted(Player player, int levels) {
        if (player instanceof ServerPlayer serverPlayer && levels > 0) {
            int skill = Rpg.data(serverPlayer).skillLevel(Skill.ENCHANTING);
            Rpg.addSkillXp(serverPlayer, Skill.ENCHANTING, levels * 8);
            if (serverPlayer.getRandom().nextInt(100) < Skill.ENCHANTING.effectValue(skill)) {
                serverPlayer.giveExperienceLevels(levels);
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("Your enchanting skill saved the levels."), true);
            }
        }
    }

    public static void onBrewed(ServerPlayer player) {
        Rpg.addSkillXp(player, Skill.BREWING, 25);
    }

    public static MobEffectInstance extendDrunkPotion(LivingEntity target, MobEffectInstance effect) {
        if (drinking != target || !(target instanceof ServerPlayer player) || effect.isInfiniteDuration() || effect.getDuration() <= 1) {
            return effect;
        }
        int brewing = Rpg.data(player).skillLevel(Skill.BREWING);
        double mult = 1 + Skill.BREWING.effectValue(brewing) / 100.0;
        if (mult <= 1) {
            return effect;
        }
        return new MobEffectInstance(effect.getEffect(), (int) (effect.getDuration() * mult), effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }
}
