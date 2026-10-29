package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.feature.blockfilter.BlockFilterFeature;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void celetweaks$filterBlock(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        if (BlockFilterFeature.shouldHide(state)) cir.setReturnValue(RenderShape.INVISIBLE);
    }
}
