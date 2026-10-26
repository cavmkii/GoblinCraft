package com.cavmkii.seancraft.entity;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.Lines;
import com.cavmkii.seancraft.PowerLevel;
import com.cavmkii.seancraft.Speech;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Joe vs Sean. When they get close they trade spells for a while: Joe throws fireballs and calls
 * meteors, Sean throws ice shards, lightning and hail. Every spell is an area blast and plenty miss,
 * so anyone nearby gets caught. Neither can drop the other below 15% health, so it's always a stalemate.
 */
public final class Fight {
    public static final float DUEL_FLOOR = 0.15f;

    public enum Spell {
        // element, radius, duel % of max health, bystander base damage, knockback, speed
        JOE_FIREBALL(Element.FIRE, 4, 0.04f, 5, 0.6f, 1.2f),
        JOE_METEOR(Element.FIRE, 6, 0.07f, 9, 1.0f, 1.6f),
        SEAN_ICE_SHARD(Element.FROST, 3.5f, 0.04f, 5, 0.5f, 1.5f),
        SEAN_LIGHTNING(Element.LIGHTNING, 5, 0.06f, 8, 0.8f, 0),
        SEAN_HAIL(Element.FROST, 2.5f, 0.015f, 2.4f, 0.3f, 1.4f);

        final Element element;
        final float radius;
        final float duelPercent;
        final float baseDamage;
        final float push;
        final float speed;

        Spell(Element element, float radius, float duelPercent, float baseDamage, float push, float speed) {
            this.element = element;
            this.radius = radius;
            this.duelPercent = duelPercent;
            this.baseDamage = baseDamage;
            this.push = push;
            this.speed = speed;
        }

        ItemStack look() {
            return switch (this) {
                case JOE_FIREBALL -> new ItemStack(Items.FIRE_CHARGE);
                case JOE_METEOR -> new ItemStack(Items.MAGMA_BLOCK);
                case SEAN_ICE_SHARD -> new ItemStack(Items.ICE);
                case SEAN_HAIL -> new ItemStack(Items.SNOWBALL);
                case SEAN_LIGHTNING -> ItemStack.EMPTY;
            };
        }
    }

    enum Element { FIRE, FROST, LIGHTNING }

    private record Pending(long at, LivingEntity caster, LivingEntity target, Spell spell) {}

    private final Joe joe;
    private final List<Pending> pending = new ArrayList<>();
    private long endsAt;
    private long cooldownUntil;
    private long nextCast;
    private boolean joeTurn;

    Fight(Joe joe) {
        this.joe = joe;
    }

    public boolean isActive(long now) {
        return endsAt > now;
    }

    static Sean findSean(Joe joe, double range) {
        List<Sean> seans = joe.level().getEntitiesOfClass(Sean.class, joe.getBoundingBox().inflate(range), Sean::isAlive);
        Sean best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Sean sean : seans) {
            double d = sean.distanceToSqr(joe);
            if (d <= range * range && d < bestDistance) {
                best = sean;
                bestDistance = d;
            }
        }
        return best;
    }

    void tryStart(long now) {
        if (now < cooldownUntil) {
            return;
        }
        Sean sean = findSean(joe, Cfg.fightTriggerRange);
        if (sean == null) {
            return;
        }
        endsAt = now + Joe.secondsToTicks(Cfg.fightSeconds);
        nextCast = now + 50;
        joe.wander().hold();
        joe.setTarget(null);

        joe.say(Lines.pick(Lines.joeFightStart()), true);
        sean.say(Lines.pick(Lines.SEAN_FIGHT_START), true);
        Speech.announce((ServerLevel) joe.level(), joe.position(), 80, "Joe has found Sean. Take cover!");
    }

    /** Called on think ticks. Returns true while a fight is running (Joe's other behaviour is suspended). */
    boolean update(long now) {
        if (endsAt <= 0) {
            return false;
        }
        Sean sean = findSean(joe, Cfg.fightTriggerRange * 2.5);
        if (now >= endsAt || sean == null) {
            finish(sean, now);
            return false;
        }
        // Hold the arena and face off.
        joe.getNavigation().stop();
        joe.getLookControl().setLookAt(sean, 30f, 30f);
        sean.getNavigation().stop();
        sean.getLookControl().setLookAt(joe, 30f, 30f);

        if (now >= nextCast) {
            nextCast = now + 18 + joe.getRandom().nextInt(26);
            castRound(sean, now);
        }
        return true;
    }

    private void finish(Sean sean, long now) {
        endsAt = 0;
        pending.clear();
        cooldownUntil = now + Joe.secondsToTicks(Cfg.fightCooldownMinutes * 60);

        joe.say(Lines.pick(Lines.joeFightEnd()), true);
        if (sean != null && sean.isAlive()) {
            sean.say(Lines.pick(Lines.SEAN_FIGHT_END), true);
        }
        // Joe storms off somewhere far away.
        Vec3 away = Wander.pickLandPoint((ServerLevel) joe.level(), joe.position(), 120, 250, joe.getRandom());
        if (away != null) {
            joe.wander().setDestination(away);
        }
    }

    private void castRound(Sean sean, long now) {
        RandomSource random = joe.getRandom();
        if (joeTurn) {
            Spell spell = random.nextFloat() < 0.65f ? Spell.JOE_FIREBALL : Spell.JOE_METEOR;
            pending.add(new Pending(now, joe, sean, spell));
        } else {
            float roll = random.nextFloat();
            Spell spell = roll < 0.45f ? Spell.SEAN_ICE_SHARD : roll < 0.8f ? Spell.SEAN_LIGHTNING : Spell.SEAN_HAIL;
            int bolts = spell == Spell.SEAN_HAIL ? 4 : 1;
            for (int i = 0; i < bolts; i++) {
                pending.add(new Pending(now + i * 5L, sean, joe, spell));
            }
        }
        // Mostly alternate; sometimes someone gets a double turn.
        if (random.nextFloat() < 0.8f) {
            joeTurn = !joeTurn;
        }
    }

    /** Called every tick during a fight to launch queued bolts on time. */
    void tickCasts(long now) {
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.at > now) {
                continue;
            }
            it.remove();
            if (p.caster.isAlive() && p.target.isAlive()) {
                launch(p.caster, p.target, p.spell);
            }
        }
    }

    private static void launch(LivingEntity caster, LivingEntity target, Spell spell) {
        ServerLevel level = (ServerLevel) caster.level();
        RandomSource random = caster.getRandom();
        Vec3 aim = target.position();
        // Plenty of misses, so the blasts spread around the arena and catch bystanders.
        float missChance = spell == Spell.SEAN_HAIL ? 0.7f : 0.35f;
        if (random.nextFloat() < missChance) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Mth.lerp(random.nextDouble(), 4, 12);
            Vec3 off = aim.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            BlockPos ground = Wander.surface(level, off);
            aim = ground != null ? Vec3.atBottomCenterOf(ground) : off;
        }

        if (spell == Spell.SEAN_LIGHTNING) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(aim);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            impact(level, caster, null, spell, aim);
            return;
        }

        Vec3 from = switch (spell) {
            case JOE_METEOR -> aim.add(random.nextDouble() * 30 - 15, 30, random.nextDouble() * 30 - 15);
            case SEAN_HAIL -> aim.add(random.nextDouble() * 8 - 4, 20, random.nextDouble() * 8 - 4);
            default -> caster.getEyePosition().add(caster.getLookAngle().scale(0.6));
        };
        SpellProjectile projectile = new SpellProjectile(level, caster, spell);
        projectile.setPos(from);
        Vec3 dir = aim.subtract(from);
        projectile.shoot(dir.x, dir.y, dir.z, spell.speed, 0f);
        level.addFreshEntity(projectile);
        level.playSound(null, from.x, from.y, from.z,
                spell.element == Element.FIRE ? SoundEvents.FIRECHARGE_USE : SoundEvents.SNOWBALL_THROW,
                SoundSource.HOSTILE, 1.2f, 0.8f + random.nextFloat() * 0.4f);
    }

    /** Area damage where a spell lands. Duelists take a slice of their own max health; bystanders take scaled damage. */
    static void impact(ServerLevel level, LivingEntity caster, Entity projectile, Spell spell, Vec3 point) {
        switch (spell.element) {
            case FIRE -> {
                level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y + 0.5, point.z, spell == Spell.JOE_METEOR ? 6 : 2, 1, 0.5, 1, 0);
                level.sendParticles(ParticleTypes.FLAME, point.x, point.y + 0.5, point.z, 40, spell.radius / 3, 0.5, spell.radius / 3, 0.05);
                level.playSound(null, point.x, point.y, point.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.5f, 1.2f);
            }
            case FROST -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y + 0.5, point.z, 40, spell.radius / 3, 0.5, spell.radius / 3, 0.05);
                level.playSound(null, point.x, point.y, point.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 1.4f);
            }
            case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 0.5, point.z, 60, spell.radius / 3, 1, spell.radius / 3, 0.2);
        }

        float powerLevel = PowerLevel.evaluate(level, point);
        AABB box = AABB.ofSize(point, spell.radius * 2, spell.radius * 2, spell.radius * 2);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            double distance = victim.position().distanceTo(point);
            if (distance > spell.radius) {
                continue;
            }
            boolean duelist = victim instanceof Joe || victim instanceof Sean;
            float damage = duelist
                    ? victim.getMaxHealth() * spell.duelPercent
                    : (float) ((spell.baseDamage + spell.baseDamage * 3f * powerLevel) * Cfg.bystanderDamageMultiplier);
            damage *= Mth.lerp((float) (distance / spell.radius), 1f, 0.4f);
            if (damage <= 0) {
                continue;
            }

            DamageSource source = projectile != null
                    ? level.damageSources().mobProjectile(projectile, caster)
                    : level.damageSources().indirectMagic(caster, caster);
            if (victim.hurt(source, damage)) {
                Vec3 push = victim.position().subtract(point).multiply(1, 0, 1);
                if (push.lengthSqr() > 1.0e-4) {
                    victim.knockback(spell.push, -push.x, -push.z);
                }
                switch (spell.element) {
                    case FIRE -> victim.igniteForSeconds(3);
                    case FROST -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), caster);
                    case LIGHTNING -> { }
                }
            }
        }
    }

    /** Joe and Sean can't finish each other off (players still can). Returns the damage allowed. */
    static float clampToDuelFloor(LivingEntity victim, float amount) {
        float floor = victim.getMaxHealth() * DUEL_FLOOR;
        float room = victim.getHealth() - floor;
        return room <= 0 ? 0 : Math.min(amount, room);
    }
}
