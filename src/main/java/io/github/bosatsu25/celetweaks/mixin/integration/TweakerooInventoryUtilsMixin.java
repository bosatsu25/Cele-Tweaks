package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.ToolSwitchScopeFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.tweakeroo.util.InventoryUtils", remap = false)
public abstract class TweakerooInventoryUtilsMixin {
    @Inject(method = "trySwitchToEffectiveTool(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private static void celetweaks$scopeToolSwitch(BlockPos pos, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client.level == null || !client.level.isLoaded(pos)) return;
        if (!ToolSwitchScopeFeature.allowSwitch(client.level.getBlockState(pos))) ci.cancel();
    }
}
