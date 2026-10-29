package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.MaterialSubstitutionFeature;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every overload of Litematica's "item needed for this block state" lookup. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialCache", remap = false)
public abstract class LitematicaMaterialCacheMixin {
    @Inject(method = "getRequiredBuildItemForState*", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void celetweaks$substitute(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack chosen = MaterialSubstitutionFeature.substitute(cir.getReturnValue());
        if (chosen != cir.getReturnValue()) cir.setReturnValue(chosen);
    }
}
