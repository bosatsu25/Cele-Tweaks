package io.github.bosatsu25.celetweaks.gametest.mixin;

import io.github.bosatsu25.celetweaks.gametest.Spies;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.SchematicBlockPicker", remap = false)
public abstract class TweakerMoreSchematicPickSpy {
    @Inject(method = "doSchematicWorldPickBlock(Lnet/minecraft/client/Minecraft;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/InteractionHand;)V",
            at = @At("RETURN"), require = 0, remap = false)
    private static void celetweaksTest$ran(Minecraft client, BlockPos pos, InteractionHand hand, CallbackInfo ci) {
        Spies.SCHEMATIC_PICK.incrementAndGet();
    }
}
