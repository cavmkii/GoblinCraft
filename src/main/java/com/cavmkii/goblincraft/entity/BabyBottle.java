package com.cavmkii.goblincraft.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** What a Bottle Grenadier throws. Shatters on whatever it hits. */
public class BabyBottle extends ThrowableItemProjectile {
    private float damage = 2;

    public BabyBottle(EntityType<? extends BabyBottle> type, Level level) {
        super(type, level);
    }

    BabyBottle(Level level, LivingEntity thrower, float damage) {
        super(ModEntities.BABY_BOTTLE, thrower, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GLASS_BOTTLE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level && !isRemoved()) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_BOTTLE)),
                    getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8f, 1.6f);
            discard();
        }
    }
}
