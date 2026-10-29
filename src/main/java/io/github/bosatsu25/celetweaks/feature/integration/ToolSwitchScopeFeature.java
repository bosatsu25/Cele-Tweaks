package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Narrows where Tweakeroo's automatic tool switch may act: only on blocks in scope, and never while the
 * player holds an item marked as "keep in hand". Tweakeroo still owns the switching.
 */
public final class ToolSwitchScopeFeature extends Feature {
    public enum Scope { ALL_EXCEPT_LISTED, ONLY_LISTED }

    public ToolSwitchScopeFeature() {
        super(FeatureDefinition.TOOL_SWITCH_SCOPE);
    }

    public static boolean allowSwitch(BlockState target) {
        if (!CeleConfig.get().isEnabled(FeatureDefinition.TOOL_SWITCH_SCOPE)) return true;
        Config cfg = CeleConfig.get().section(FeatureDefinition.TOOL_SWITCH_SCOPE, Config.class);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && IdMatcher.of(cfg.keepInHand).matches(client.player.getMainHandItem())) return false;
        boolean listed = IdMatcher.of(cfg.blocks).matches(target);
        return cfg.scope == Scope.ONLY_LISTED ? listed : !listed;
    }

    public static final class Config {
        public Scope scope = Scope.ALL_EXCEPT_LISTED;
        /** Block ids or #tags. */
        public List<String> blocks = new ArrayList<>();
        /** Item ids or #tags that are never switched away from. */
        public List<String> keepInHand = new ArrayList<>();
    }
}
