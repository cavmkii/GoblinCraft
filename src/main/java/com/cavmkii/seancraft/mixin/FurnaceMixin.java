package com.cavmkii.seancraft.mixin;

import com.cavmkii.seancraft.tarkov.Tarkov;
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
    private static void seancraft$workshop(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        FurnaceAccessor f = (FurnaceAccessor) furnace;
        int progress = f.seancraft$cookingProgress();
        int total = f.seancraft$cookingTotalTime();
        if (f.seancraft$litTime() <= 0 || progress <= 0 || progress >= total - 1 || !(level instanceof ServerLevel server)) {
            return;
        }
        double bonus = Tarkov.furnaceBonus(server, pos);
        if (bonus <= 0) {
            return;
        }
        int extra = (int) bonus + (level.random.nextDouble() < bonus - (int) bonus ? 1 : 0);
        // Stop one short so vanilla finishes the item on its own next tick.
        f.seancraft$setCookingProgress(Math.min(total - 1, progress + extra));
    }
}
