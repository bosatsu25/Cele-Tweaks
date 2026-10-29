package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.feature.lava.LavaHighlightFeature;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Same signatures on 26.1.2, 26.2 and 26.3 (from the CI signature dump). */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    private static final String TESSELATE = "tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V";

    @Inject(method = TESSELATE, at = @At("HEAD"))
    private void celetweaks$beginFluid(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output,
                                       BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        LavaHighlightFeature.beginFluid(fluidState);
    }

    @Inject(method = TESSELATE, at = @At("RETURN"))
    private void celetweaks$endFluid(CallbackInfo ci) {
        LavaHighlightFeature.endFluid();
    }

    @ModifyVariable(method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFIFFI)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int celetweaks$tintLava(int color) {
        return LavaHighlightFeature.tintVertex(color);
    }
}
