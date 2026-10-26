package com.cavmkii.seancraft.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/** Food you drink. */
public class SodaItem extends Item {
    public SodaItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return getDrinkingSound();
    }
}
