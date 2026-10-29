package io.github.bosatsu25.celetweaks.feature.blockfilter;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.config.ListMode;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Hides blocks by id or #tag (or shows only those) by meshing them as invisible. */
public final class BlockFilterFeature extends Feature {
    private static volatile boolean active;
    private static volatile ListMode mode = ListMode.HIDE;
    private static volatile IdMatcher matcher = IdMatcher.EMPTY;

    public BlockFilterFeature() {
        super(FeatureDefinition.BLOCK_FILTER);
    }

    @Override
    public void init() { refresh(false); }

    @Override
    public void onToggle(FeatureDefinition definition, boolean enabled) { refresh(true); }

    @Override
    public void onQuarantined(Minecraft client) {
        active = false;
        Compat.reloadChunks(client);
    }

    private void refresh(boolean remesh) {
        Config cfg = config(Config.class);
        mode = cfg.mode == null ? ListMode.HIDE : cfg.mode;
        matcher = IdMatcher.of(cfg.entries == null ? List.of() : cfg.entries);
        active = CeleConfig.get().isEnabled(definition);
        if (remesh) Compat.reloadChunks(Minecraft.getInstance());
    }

    /** Hot path: called while chunks are meshed. */
    public static boolean shouldHide(BlockState state) {
        return active && mode.affects(matcher.matches(state));
    }

    public static final class Config {
        public ListMode mode = ListMode.HIDE;
        /** Block ids or #tags, e.g. "minecraft:glass", "#minecraft:leaves". */
        public List<String> entries = new ArrayList<>();
    }
}
