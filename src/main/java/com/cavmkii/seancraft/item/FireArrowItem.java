package com.cavmkii.seancraft.item;

import com.cavmkii.seancraft.combat.Damage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Fire arrow (design doc 9.2): pierce damage with a fire share, and it sets what it hits alight. */
public class FireArrowItem extends ArrowItem {
    public FireArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        AbstractArrow arrow = super.createArrow(level, ammo, shooter, weapon);
        arrow.addTag(Damage.FIRE_ARROW_TAG);
        arrow.igniteForSeconds(100);
        return arrow;
    }
}
