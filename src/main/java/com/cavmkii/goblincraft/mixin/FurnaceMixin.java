package com.cavmkii.goblincraft.mixin;

import com.cavmkii.goblincraft.tarkov.Tarkov;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hideout Workshop: furnaces inside a hideout cook faster. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceMixin {
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void goblincraft$workshop(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        FurnaceAccessor f = (FurnaceAccessor) furnace;
        int progress = f.goblincraft$cookingProgress();
        int total = f.goblincraft$cookingTotalTime();
        if (f.goblincraft$litTime() <= 0 || progress <= 0 || progress >= total - 1 || !(level instanceof ServerLevel server)) {
            return;
        }
        double bonus = Tarkov.furnaceBonus(server, pos);
        if (bonus <= 0) {
            return;
        }
        int extra = (int) bonus + (level.random.nextDouble() < bonus - (int) bonus ? 1 : 0);
        // Stop one short so vanilla finishes the item on its own next tick.
        f.goblincraft$setCookingProgress(Math.min(total - 1, progress + extra));
    }
}
