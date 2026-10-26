package com.cavmkii.seancraft.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A vomiting fit: the mob stands still for a few seconds spraying green slop from its mouth. */
public final class Puke {
    public static final int TICKS = 80;

    private static final ItemParticleOption SLOP = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL));

    private final Mob mob;
    private int remaining;

    public Puke(Mob mob) {
        this.mob = mob;
    }

    public boolean active() {
        return remaining > 0;
    }

    public void start() {
        remaining = TICKS;
        mob.getNavigation().stop();
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /** Call every server tick. */
    public void tick() {
        if (remaining <= 0 || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        remaining--;
        mob.getNavigation().stop();
        Vec3 look = mob.getLookAngle();
        Vec3 mouth = mob.getEyePosition().add(look.scale(0.3 * mob.getScale())).add(0, -0.15 * mob.getScale(), 0);
        for (int i = 0; i < 3; i++) {
            double spread = 0.15;
            Vec3 dir = look.add((mob.getRandom().nextDouble() - 0.5) * spread, -0.3 + (mob.getRandom().nextDouble() - 0.5) * spread,
                    (mob.getRandom().nextDouble() - 0.5) * spread);
            // Count 0 makes the offset a velocity.
            level.sendParticles(SLOP, mouth.x, mouth.y, mouth.z, 0, dir.x, dir.y, dir.z, 0.25);
        }
        if (remaining % 20 == 0) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 0.8f, 0.7f);
        }
    }
}
