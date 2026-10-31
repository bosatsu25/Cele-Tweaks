package io.github.bosatsu25.celetweaks.mixin.version;

import io.github.bosatsu25.celetweaks.feature.sidebar.SidebarLayoutFeature;
import io.github.bosatsu25.celetweaks.feature.sidebar.SidebarLayoutRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3: the sidebar moved from Gui to Hud.displayScoreboardSidebar(GuiGraphicsExtractor, Objective). */
@Mixin(Hud.class)
public abstract class GuiScoreboardMixin {
    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"), cancellable = true)
    private void celetweaks$replaceSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        SidebarLayoutFeature feature = SidebarLayoutFeature.instance();
        if (feature == null || !feature.isEnabled()) return;
        SidebarLayoutRenderer.render(graphics, objective, feature);
        ci.cancel();
    }
}
