package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.rpg.RpgDamage;
import com.cavmkii.seancraft.rpg.RpgHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Character damage rules, before vanilla armor, blocking and invulnerability. */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float seancraft$modifyDamage(float amount, DamageSource source) {
        return RpgDamage.modify((LivingEntity) (Object) this, source, amount);
    }

    /** Brewing skill: potions you drink last longer. */
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance seancraft$longerPotions(MobEffectInstance effect) {
        return RpgHooks.extendDrunkPotion((LivingEntity) (Object) this, effect);
    }
}
