package com.cavmkii.seancraft.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceAccessor {
    @Accessor("litTime")
    int seancraft$litTime();

    @Accessor("cookingProgress")
    int seancraft$cookingProgress();

    @Accessor("cookingProgress")
    void seancraft$setCookingProgress(int value);

    @Accessor("cookingTotalTime")
    int seancraft$cookingTotalTime();
}
