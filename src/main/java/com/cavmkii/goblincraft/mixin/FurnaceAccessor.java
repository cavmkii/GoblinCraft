package com.cavmkii.goblincraft.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceAccessor {
    @Accessor("litTime")
    int goblincraft$litTime();

    @Accessor("cookingProgress")
    int goblincraft$cookingProgress();

    @Accessor("cookingProgress")
    void goblincraft$setCookingProgress(int value);

    @Accessor("cookingTotalTime")
    int goblincraft$cookingTotalTime();
}
