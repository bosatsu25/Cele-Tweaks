package io.github.bosatsu25.celetweaks.mixin.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition.Mods;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a mixin against an external mod only when that mod is actually loaded, and counts the call
 * sites each one wove into its target. These mixins use {@code require = 0}, so a target renamed by
 * the other mod cannot crash the game; without the count that would leave the feature silently idle.
 */
public final class IntegrationMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("celetweaks");
    private static final String PREFIX = "io.github.bosatsu25.celetweaks.mixin.integration.";
    /** Every handler method in this package is named {@code celetweaks$...}. */
    private static final String HANDLER_PREFIX = "celetweaks$";
    private static final Map<String, String> OWNERS = Map.of(
            PREFIX + "TweakerooInventoryUtilsMixin", Mods.TWEAKEROO,
            PREFIX + "TweakerMoreSchematicBlockPickerMixin", Mods.TWEAKERMORE,
            PREFIX + "LitematicaMaterialCacheMixin", Mods.LITEMATICA,
            PREFIX + "ItemScrollerKeybindCallbacksMixin", Mods.ITEMSCROLLER,
            PREFIX + "SyncmaticaRemoveButtonMixin", Mods.SYNCMATICA,
            PREFIX + "TweakerMorePlacementRestrictionMixin", Mods.TWEAKERMORE,
            PREFIX + "TweakerMoreLooseCaseMixin", Mods.TWEAKERMORE,
            PREFIX + "TweakerMoreInteractionRestrictorMixin", Mods.TWEAKERMORE);

    private static final Map<String, String> SELECTED = new ConcurrentHashMap<>();
    private static final Map<String, Integer> HOOKS = new ConcurrentHashMap<>();

    /** Mixins selected because their mod is loaded: mixin simple name → target class. */
    public static Map<String, String> selected() { return Map.copyOf(SELECTED); }

    /** Call sites woven so far (a target is woven when it is first loaded): mixin simple name → count. */
    public static Map<String, Integer> hooks() { return Map.copyOf(HOOKS); }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String owner = OWNERS.get(mixinClassName);
        boolean apply = owner != null && FabricLoader.getInstance().isModLoaded(owner);
        if (apply) SELECTED.put(simpleName(mixinClassName), targetClassName);
        return apply;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Set<String> handlers = new HashSet<>();
        for (MethodNode method : mixinInfo.getClassNode(0).methods) {
            if (method.name.startsWith(HANDLER_PREFIX)) handlers.add(method.name);
        }
        int calls = 0;
        for (MethodNode method : targetClass.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals(targetClass.name) && isHandler(call.name, handlers)) {
                    calls++;
                }
            }
        }
        String name = simpleName(mixinClassName);
        HOOKS.put(name, calls);
        if (calls == 0) {
            LOGGER.warn("{} found nothing to hook in {}; its feature does nothing with this version of the mod",
                    name, targetClassName);
        }
    }

    /** Mixin merges a handler under its own name or a generated {@code handler$...$<name>} one. */
    private static boolean isHandler(String invoked, Set<String> handlers) {
        if (handlers.contains(invoked)) return true;
        for (String handler : handlers) {
            if (invoked.endsWith("$" + handler)) return true;
        }
        return false;
    }

    private static String simpleName(String mixinClassName) {
        return mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
    }
}
