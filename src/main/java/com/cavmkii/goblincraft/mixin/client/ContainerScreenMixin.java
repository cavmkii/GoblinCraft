package com.cavmkii.goblincraft.mixin.client;

import com.cavmkii.goblincraft.client.tarkov.GridPanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shows the Tetris grids beside vanilla container screens. */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    private AbstractContainerScreen<?> goblincraft$self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void goblincraft$renderGrids(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (GridPanel.applies(goblincraft$self())) {
            GridPanel.render(goblincraft$self(), g, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void goblincraft$clickGrids(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (GridPanel.applies(goblincraft$self())
                && GridPanel.click(goblincraft$self(), mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void goblincraft$releaseGrids(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (GridPanel.release()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void goblincraft$keyGrids(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (GridPanel.applies(goblincraft$self()) && GridPanel.key(goblincraft$self(), keyCode)) {
            cir.setReturnValue(true);
        }
    }
}
