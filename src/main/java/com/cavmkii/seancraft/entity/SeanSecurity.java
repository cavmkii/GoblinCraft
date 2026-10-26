package com.cavmkii.seancraft.entity;

import com.cavmkii.seancraft.world.WorldDirector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One of Sean's Security guards. Stays near Sean (or the war camp) and fights the babies.
 * Leaves people in Sean's Merch alone and goes after everyone else. Only exists during a war.
 */
public class SeanSecurity extends PathfinderMob {
    private final Wander wander = new Wander(this);
    private long nextThink;

    public SeanSecurity(EntityType<? extends SeanSecurity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 3;
        if (!level.isClientSide) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                setDropChance(slot, 0f);
            }
            setCustomName(Component.literal("Sean's Security"));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    public static boolean isHostileTo(Player player) {
        return !player.isCreative() && !player.isSpectator() && !Joe.wearsMerch(player);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // Joe is left alone: at low levels three guards would kill him in seconds and end the war.
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SeanSecurity.class, Sean.class, Joe.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, BabyGoblin.class, 10, false, false, null));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && isHostileTo(player)));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        WorldDirector director = WorldDirector.get();
        long now = level().getGameTime();
        if (now < nextThink) {
            return;
        }
        nextThink = now + 20;
        if (director == null || !director.war.isActive()) {
            discard();
            return;
        }
        if (getTarget() instanceof Player player && !isHostileTo(player)) {
            setTarget(null);
        }
        if (getTarget() == null) {
            Sean sean = director.sean();
            boolean nearSean = sean != null && sean.distanceToSqr(this) < 120 * 120;
            Vec3 post = nearSean ? sean.position() : director.war.camp();
            wander.setAnchor(post, nearSean ? 6 : 15);
            wander.step(1.0);
        }
    }
}
