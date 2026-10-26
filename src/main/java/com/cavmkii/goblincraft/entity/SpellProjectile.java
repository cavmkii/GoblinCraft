package com.cavmkii.goblincraft.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A duel spell in flight. Rendered as its item; bursts into area damage wherever it lands. Never breaks blocks. */
public class SpellProjectile extends ThrowableItemProjectile {
    private static final int MAX_AGE = 100;

    private Fight.Spell spell = Fight.Spell.JOE_FIREBALL;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    SpellProjectile(Level level, LivingEntity caster, Fight.Spell spell) {
        super(ModEntities.SPELL, caster, level);
        this.spell = spell;
        setItem(spell.look());
        // Fireballs fly straight; ice and hail drop a little.
        setNoGravity(spell.element == Fight.Element.FIRE);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
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
        // Don't burst on other spells or on the caster's own allies' projectiles.
        return !(target instanceof SpellProjectile) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Damage is applied by the burst in onHit, not by the direct hit.
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level && !isRemoved()) {
            LivingEntity caster = getOwner() instanceof LivingEntity owner ? owner : null;
            if (caster != null) {
                Fight.impact(level, caster, this, spell, result.getLocation());
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", spell.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            spell = Fight.Spell.valueOf(tag.getString("Spell"));
        } catch (IllegalArgumentException e) {
            spell = Fight.Spell.JOE_FIREBALL;
        }
    }
}
