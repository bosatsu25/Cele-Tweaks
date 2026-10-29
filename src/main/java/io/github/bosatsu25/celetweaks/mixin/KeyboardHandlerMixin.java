package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds raw key presses to the hotkey manager (no vanilla KeyMapping per feature). */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    // keyPress(long window, int action, KeyEvent event) on 26.1.2, 26.2 and 26.3.
    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"))
    private void celetweaks$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        HotkeyManager.onKey(event.key(), action);
    }
}
