package com.cavmkii.seancraft.mixin.client;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The chat's wrapped lines, so the status HUD can stay clear of whatever chat is showing. */
@Mixin(ChatComponent.class)
public interface ChatAccessor {
    @Accessor("trimmedMessages")
    List<GuiMessage.Line> seancraft$lines();
}
