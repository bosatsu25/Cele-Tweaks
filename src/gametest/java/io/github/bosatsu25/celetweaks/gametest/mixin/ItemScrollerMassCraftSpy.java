package io.github.bosatsu25.celetweaks.gametest.mixin;

import io.github.bosatsu25.celetweaks.gametest.Spies;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.itemscroller.event.KeybindCallbacks", remap = false)
public abstract class ItemScrollerMassCraftSpy {
    @Inject(method = "onClientTickMassCraftImpl(Lnet/minecraft/client/Minecraft;)V", at = @At("RETURN"), require = 0, remap = false)
    private void celetweaksTest$ran(Minecraft client, CallbackInfo ci) {
        Spies.MASS_CRAFT.incrementAndGet();
    }
}
