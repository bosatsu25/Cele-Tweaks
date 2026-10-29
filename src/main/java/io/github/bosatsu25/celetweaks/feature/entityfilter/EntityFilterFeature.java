package io.github.bosatsu25.celetweaks.feature.entityfilter;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.config.ListMode;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Hides entities by id or #tag (or shows only those). Rendering only; entities keep existing. */
public final class EntityFilterFeature extends Feature {
    private static volatile boolean active;
    private static volatile ListMode mode = ListMode.HIDE;
    private static volatile IdMatcher matcher = IdMatcher.EMPTY;
    private static volatile boolean keepPlayers = true;

    public EntityFilterFeature() {
        super(FeatureDefinition.ENTITY_FILTER);
    }

    @Override
    public void init() { refresh(); }

    @Override
    public void onToggle(FeatureDefinition definition, boolean enabled) { refresh(); }

    private void refresh() {
        Config cfg = config(Config.class);
        mode = cfg.mode == null ? ListMode.HIDE : cfg.mode;
        matcher = IdMatcher.of(cfg.entries == null ? List.of() : cfg.entries);
        keepPlayers = cfg.alwaysShowPlayers;
        active = CeleConfig.get().isEnabled(definition);
    }

    /** Hot path: called for every entity every frame. */
    public static boolean shouldHide(Entity entity) {
        if (!active || entity == Minecraft.getInstance().player) return false;
        if (keepPlayers && entity instanceof Player) return false;
        return mode.affects(matcher.matches(entity.getType()));
    }

    public static final class Config {
        public ListMode mode = ListMode.HIDE;
        /** Entity ids or #tags, e.g. "minecraft:item", "#minecraft:skeletons". */
        public List<String> entries = new ArrayList<>();
        public boolean alwaysShowPlayers = true;
    }
}
