package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.CraftBurstGuardFeature;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only the mass-craft step of Item Scroller's client tick. The tick itself also flushes Item Scroller's
 * buffered click packets, which must keep running while the inventory is open.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.itemscroller.event.KeybindCallbacks", remap = false)
public abstract class ItemScrollerKeybindCallbacksMixin {
    @Inject(method = "onClientTickMassCraftImpl(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private void celetweaks$guardMassCraft(Minecraft client, CallbackInfo ci) {
        if (CraftBurstGuardFeature.shouldBlock()) ci.cancel();
    }
}
