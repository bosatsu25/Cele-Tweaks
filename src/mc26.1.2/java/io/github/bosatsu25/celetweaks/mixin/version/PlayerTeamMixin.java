package io.github.bosatsu25.celetweaks.mixin.version;

import io.github.bosatsu25.celetweaks.feature.quiet.QuietUiFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.1.2: PlayerTeam.getColor() returns ChatFormatting. */
@Mixin(PlayerTeam.class)
public abstract class PlayerTeamMixin {
    @Inject(method = "getColor()Lnet/minecraft/ChatFormatting;", at = @At("HEAD"), cancellable = true)
    private void celetweaks$quietTeamColor(CallbackInfoReturnable<ChatFormatting> cir) {
        if (QuietUiFeature.neutralTeams()) cir.setReturnValue(ChatFormatting.GRAY);
    }
}
