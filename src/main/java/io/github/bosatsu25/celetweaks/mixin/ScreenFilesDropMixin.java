package io.github.bosatsu25.celetweaks.mixin;

import io.github.bosatsu25.celetweaks.feature.integration.SchematicDragDropFeature;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

/** Litematica screens inherit Screen.onFilesDrop; handle drops there only for its schematic browsers. */
@Mixin(Screen.class)
public abstract class ScreenFilesDropMixin {
    @Inject(method = "onFilesDrop(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void celetweaks$schematicDrop(List<Path> paths, CallbackInfo ci) {
        if (SchematicDragDropFeature.onFilesDrop((Screen) (Object) this, paths)) ci.cancel();
    }
}
