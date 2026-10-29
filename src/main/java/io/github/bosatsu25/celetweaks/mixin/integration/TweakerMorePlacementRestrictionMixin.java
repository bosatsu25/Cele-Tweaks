package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TweakerMore's "is this the right block to place here" check; Cele may only turn a "no" into a "yes". */
@Pseudo
@Mixin(targets = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.restrict.PlacementRestrictor", remap = false)
public abstract class TweakerMorePlacementRestrictionMixin {
    @Inject(method = "isBlockToPlaceCorrect(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void celetweaks$standIn(BlockState schematic, BlockState toPlace, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && PlacementConversionsFeature.active()
                && PlacementConversionsFeature.acceptsStandIn(schematic.getBlock(), toPlace.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
