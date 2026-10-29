package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The item-based variant of TweakerMore's placement check; stand-in rules apply here too. */
@Pseudo
@Mixin(targets = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.restrict.LooseCaseChecker", remap = false)
public abstract class TweakerMoreLooseCaseMixin {
    @Inject(method = "isLooseCheckSpecialCase(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void celetweaks$standInItem(BlockState schematic, ItemStack schematicStack, ItemStack toUse,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !PlacementConversionsFeature.active()) return;
        if (toUse.getItem() instanceof BlockItem blockItem
                && PlacementConversionsFeature.acceptsStandIn(schematic.getBlock(), blockItem.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
