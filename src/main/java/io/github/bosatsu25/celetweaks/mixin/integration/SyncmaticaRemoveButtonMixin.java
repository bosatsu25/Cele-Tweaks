package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.SharedPlanLockFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Syncmatica's server placement entry buttons share one listener class; only its REMOVE type is guarded. */
@Pseudo
@Mixin(targets = "ch.endte.syncmatica.litematica.gui.WidgetSyncmaticaServerPlacementEntry$ButtonListener", remap = false)
public abstract class SyncmaticaRemoveButtonMixin {
    @Inject(method = "actionPerformedWithButton", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void celetweaks$guardRemove(CallbackInfo ci) {
        if (SharedPlanLockFeature.shouldBlock(this)) ci.cancel();
    }
}
