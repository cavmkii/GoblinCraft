package com.cavmkii.seancraft.boss;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A projectile from a boss attack, drawn as its item. Its hit is the attack's hit, once per player per attack. */
public class BossShot extends ThrowableItemProjectile {
    private static final int MAX_AGE = 40;
    private BossDef.Attack attack;

    public BossShot(EntityType<? extends BossShot> type, Level level) {
        super(type, level);
    }

    BossShot(Level level, BossEntity boss, BossDef.Attack attack, ItemStack look) {
        super(BossContent.BOSS_SHOT, boss, level);
        this.attack = attack;
        setItem(look);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PAPER;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target instanceof ServerPlayer && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (attack != null && getOwner() instanceof BossEntity boss && result.getEntity() instanceof ServerPlayer player) {
            boss.shotHit(player, attack);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
