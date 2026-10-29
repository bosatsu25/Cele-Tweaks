package io.github.bosatsu25.celetweaks.mixin.version;

import io.github.bosatsu25.celetweaks.feature.quiet.QuietUiFeature;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** 26.3: PlayerTeam.getColor() returns Optional<TeamColor>; every team is shown in neutral grey. */
@Mixin(PlayerTeam.class)
public abstract class PlayerTeamMixin {
    @Inject(method = "getColor()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void celetweaks$quietTeamColor(CallbackInfoReturnable<Optional<TeamColor>> cir) {
        if (QuietUiFeature.neutralTeams()) cir.setReturnValue(Optional.of(TeamColor.GRAY));
    }
}
