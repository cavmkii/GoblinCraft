package com.cavmkii.seancraft.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets players rename chests and barrels from the GEAR tab (vanilla only names them through an anvil). */
@Mixin(BaseContainerBlockEntity.class)
public interface ContainerNameAccessor {
    @Accessor("name")
    void seancraft$setName(Component name);
}
