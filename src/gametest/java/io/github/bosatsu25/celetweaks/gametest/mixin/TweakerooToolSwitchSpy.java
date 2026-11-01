package io.github.bosatsu25.celetweaks.gametest.mixin;

import io.github.bosatsu25.celetweaks.gametest.Spies;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.tweakeroo.util.InventoryUtils", remap = false)
public abstract class TweakerooToolSwitchSpy {
    @Inject(method = "trySwitchToEffectiveTool(Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"), require = 0, remap = false)
    private static void celetweaksTest$ran(BlockPos pos, CallbackInfo ci) {
        Spies.TOOL_SWITCH.incrementAndGet();
    }
}
