package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TweakerMore's interaction check returns its own result type, handled here as Object; the "accepted"
 * value is obtained from that type's static good() factory by reflection.
 */
@Pseudo
@Mixin(targets = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.restrict.BlockInteractionRestrictor", remap = false)
public abstract class TweakerMoreInteractionRestrictorMixin {
    @Inject(method = "checkInteract", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void celetweaks$conversion(Player player, BlockState world, BlockState schematic,
                                             CallbackInfoReturnable<Object> cir) {
        Object verdict = cir.getReturnValue();
        if (verdict == null || !PlacementConversionsFeature.active()
                || !PlacementConversionsFeature.acceptsConversion(player, world, schematic)) return;
        try {
            Object accepted = verdict.getClass().getMethod("good").invoke(null);
            if (!accepted.equals(verdict)) cir.setReturnValue(accepted);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Unknown TweakerMore result API: keep its verdict.
        }
    }
}
