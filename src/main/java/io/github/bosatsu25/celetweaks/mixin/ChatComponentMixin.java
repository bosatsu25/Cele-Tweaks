package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.feature.grey.GreyColorsFeature;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    // Same descriptor on 26.1.2, 26.2 and 26.3 (verified from the CI signature dump).
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component celetweaks$greyChat(Component message) {
        return GreyColorsFeature.greyChat() ? GreyColorsFeature.toGreyscale(message) : message;
    }
}
