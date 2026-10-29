package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition.Mods;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies a mixin against an external mod only when that mod is actually loaded. */
public final class IntegrationMixinPlugin implements IMixinConfigPlugin {
    private static final String PREFIX = "io.github.bosatsu25.celetweaks.mixin.integration.";
    private static final Map<String, String> OWNERS = Map.of(
            PREFIX + "TweakerooInventoryUtilsMixin", Mods.TWEAKEROO,
            PREFIX + "TweakerMoreSchematicBlockPickerMixin", Mods.TWEAKERMORE,
            PREFIX + "LitematicaMaterialCacheMixin", Mods.LITEMATICA,
            PREFIX + "ItemScrollerKeybindCallbacksMixin", Mods.ITEMSCROLLER,
            PREFIX + "SyncmaticaRemoveButtonMixin", Mods.SYNCMATICA,
            PREFIX + "TweakerMorePlacementRestrictionMixin", Mods.TWEAKERMORE,
            PREFIX + "TweakerMoreLooseCaseMixin", Mods.TWEAKERMORE,
            PREFIX + "TweakerMoreInteractionRestrictorMixin", Mods.TWEAKERMORE);

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String owner = OWNERS.get(mixinClassName);
        return owner != null && FabricLoader.getInstance().isModLoaded(owner);
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
