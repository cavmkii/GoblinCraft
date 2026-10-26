package com.cavmkii.seancraft.entity;

import com.cavmkii.seancraft.Cfg;
import com.cavmkii.seancraft.PowerLevel;
import com.cavmkii.seancraft.world.WorldDirector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A soldier in Joe's baby army: a goblin toddler. Diaper Commandos and Binky Berserkers brawl, Bottle
 * Grenadiers throw bottles. They go for the nearest player not wearing a Baby Bonnet, scale with
 * progression like Joe, and carry whatever curses were offered at the war cauldron. Only exist during
 * a war; any left over remove themselves.
 */
public class BabyGoblin extends PathfinderMob implements RangedAttackMob {
    public static final int CURSE_PUKE = 1;
    public static final int CURSE_SLOW = 2;
    public static final int CURSE_HEAVY = 4;

    private final Wander wander = new Wander(this);
    private final Puke puke = new Puke(this);
    private int curses;
    private long nextThink;
    private long nextScale;
    private long nextPuke;

    public BabyGoblin(EntityType<? extends BabyGoblin> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 2;
        if (!level.isClientSide) {
            AttributeInstance scale = getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(type == ModEntities.BABY_BERSERKER ? 1.5 : type == ModEntities.BABY_GRENADIER ? 1.1 : 1.0);
            }
            if (type == ModEntities.BABY_GRENADIER) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GLASS_BOTTLE));
            } else if (type == ModEntities.BABY_BERSERKER) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            }
            setDropChance(EquipmentSlot.MAINHAND, 0f);
            nextPuke = level.getGameTime() + 120 + getRandom().nextInt(180);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Joe.BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, Joe.BASE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    /** Always toddler-sized: renders with big-headed baby proportions at half scale. */
    @Override
    public boolean isBaby() {
        return true;
    }

    @Override
    protected void registerGoals() {
        // registerGoals runs inside the Mob constructor, but the entity type is already set.
        goalSelector.addGoal(0, new FloatGoal(this));
        if (getType() == ModEntities.BABY_GRENADIER) {
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 12f));
        } else {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, getType() == ModEntities.BABY_BERSERKER ? 1.35 : 1.2, false));
        }
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, BabyGoblin.class, Joe.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                target -> target instanceof Player player && isHostileTo(player)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, SeanSecurity.class, 10, false, false, null));
    }

    /** Babies leave Bonnet wearers alone. */
    public static boolean isHostileTo(Player player) {
        return !player.isCreative() && !player.isSpectator() && !Joe.wearsBonnet(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Applied once at spawn by the war. Curses change movement and damage. */
    public void applyCurses(int mask) {
        curses = mask;
        double speed = 1;
        if ((mask & CURSE_SLOW) != 0) {
            speed *= 0.5;
        }
        if ((mask & CURSE_HEAVY) != 0) {
            speed *= 0.7;
        }
        AttributeInstance movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && speed < 1) {
            movement.setBaseValue(movement.getBaseValue() * speed);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        WorldDirector director = WorldDirector.get();
        long now = level().getGameTime();
        puke.tick();
        if (now < nextThink) {
            return;
        }
        nextThink = now + 20;

        if (director == null || !director.war.isActive()) {
            discard();
            return;
        }
        updateScaling(now);

        if (getTarget() instanceof Player player && !isHostileTo(player)) {
            setTarget(null);
        }

        if ((curses & CURSE_PUKE) != 0 && updatePuke(now)) {
            return;
        }

        if (getTarget() == null) {
            Player prey = closestEnemyPlayer(80);
            if (prey != null) {
                getNavigation().moveTo(prey, 1.1);
            } else {
                wander.setAnchor(director.war.camp(), 10);
                wander.step(0.9);
            }
        }
    }

    private Player closestEnemyPlayer(double range) {
        Player best = null;
        double bestDistance = range * range;
        for (Player player : level().players()) {
            if (!player.isAlive() || !isHostileTo(player)) {
                continue;
            }
            double d = player.distanceToSqr(this);
            if (d < bestDistance) {
                best = player;
                bestDistance = d;
            }
        }
        return best;
    }

    private boolean updatePuke(long now) {
        if (puke.active()) {
            return true;
        }
        if (now < nextPuke) {
            return false;
        }
        nextPuke = now + 160 + getRandom().nextInt(160);
        puke.start();
        return true;
    }

    private void updateScaling(long now) {
        if (now < nextScale) {
            return;
        }
        nextScale = now + 400;
        float level = PowerLevel.evaluate((ServerLevel) level(), position());
        double hp = Joe.BASE_HEALTH * PowerLevel.lerp(Cfg.healthMultMin, Cfg.healthMultMax, level) * Cfg.babyHealthScale;
        double dmg = Joe.BASE_DAMAGE * PowerLevel.lerp(Cfg.damageMultMin, Cfg.damageMultMax, level) * Cfg.babyDamageScale;
        if ((curses & CURSE_HEAVY) != 0) {
            dmg *= 0.6;
        }
        if (getType() == ModEntities.BABY_BERSERKER) {
            hp *= 1.6;
            dmg *= 1.4;
        }
        Joe.applyScaling(this, hp, dmg);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        BabyBottle bottle = new BabyBottle(level(), this, damage);
        Vec3 to = target.getEyePosition().subtract(bottle.position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        bottle.shoot(to.x, to.y + flat * 0.2, to.z, 1.1f, 8f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.HOSTILE, 1f, 1.4f);
        level().addFreshEntity(bottle);
    }
}
