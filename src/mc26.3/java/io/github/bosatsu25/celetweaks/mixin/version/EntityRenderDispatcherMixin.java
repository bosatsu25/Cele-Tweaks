package io.github.bosatsu25.celetweaks.mixin.version;

import io.github.bosatsu25.celetweaks.feature.entityfilter.EntityFilterFeature;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.3: shouldRender(entity, frustum, x, y, z, partialTick). */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDF)Z", at = @At("HEAD"), cancellable = true)
    private void celetweaks$filterEntity(Entity entity, Frustum frustum, double x, double y, double z, float partialTick,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (EntityFilterFeature.shouldHide(entity)) cir.setReturnValue(false);
    }
}
