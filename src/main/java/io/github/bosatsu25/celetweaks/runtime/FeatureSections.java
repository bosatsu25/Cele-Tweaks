package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.feature.blockfilter.BlockFilterFeature;
import io.github.bosatsu25.celetweaks.feature.entityfilter.EntityFilterFeature;
import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature;
import io.github.bosatsu25.celetweaks.feature.guard.NoDigUnderfootFeature;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.AutoPickScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.MaterialSubstitutionFeature;
import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SchematicDragDropFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncerTuneFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncmaticaRemoveLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.ToolSwitchScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.TweakerooResyncFeature;
import io.github.bosatsu25.celetweaks.feature.lava.LavaSourceTintFeature;
import io.github.bosatsu25.celetweaks.feature.log.ActionLogFeature;
import io.github.bosatsu25.celetweaks.feature.look.SlowLookFeature;
import io.github.bosatsu25.celetweaks.feature.range.RangeOverlayFeature;
import io.github.bosatsu25.celetweaks.feature.sidebar.SidebarLayoutFeature;

import java.util.Map;

/** Maps a feature switch to the config section class the settings screen edits. */
public final class FeatureSections {
    private static final Map<FeatureDefinition, Class<?>> SECTIONS = Map.ofEntries(
            Map.entry(FeatureDefinition.SIDEBAR_LAYOUT, SidebarLayoutFeature.Config.class),
            Map.entry(FeatureDefinition.NO_DIG_UNDERFOOT, NoDigUnderfootFeature.Config.class),
            Map.entry(FeatureDefinition.ITEM_USE_GUARD, ItemUseGuardFeature.Config.class),
            Map.entry(FeatureDefinition.HOTBAR_LOCK, HotbarLockFeature.Config.class),
            Map.entry(FeatureDefinition.SLOW_LOOK, SlowLookFeature.Config.class),
            Map.entry(FeatureDefinition.ACTION_LOG, ActionLogFeature.Config.class),
            Map.entry(FeatureDefinition.RANGE_OVERLAY, RangeOverlayFeature.Config.class),
            Map.entry(FeatureDefinition.ENTITY_FILTER, EntityFilterFeature.Config.class),
            Map.entry(FeatureDefinition.BLOCK_FILTER, BlockFilterFeature.Config.class),
            Map.entry(FeatureDefinition.LAVA_SOURCE_TINT, LavaSourceTintFeature.Config.class),
            Map.entry(FeatureDefinition.SYNCMATICA_REMOVE_LOCK, SyncmaticaRemoveLockFeature.Config.class),
            Map.entry(FeatureDefinition.TOOL_SWITCH_SCOPE, ToolSwitchScopeFeature.Config.class),
            Map.entry(FeatureDefinition.TWEAKEROO_RESYNC, TweakerooResyncFeature.Config.class),
            Map.entry(FeatureDefinition.AUTO_PICK_SCOPE, AutoPickScopeFeature.Config.class),
            Map.entry(FeatureDefinition.SYNCER_TUNE, SyncerTuneFeature.Config.class),
            Map.entry(FeatureDefinition.PLACEMENT_CONVERSIONS, PlacementConversionsFeature.Config.class),
            Map.entry(FeatureDefinition.MATERIAL_SUBSTITUTION, MaterialSubstitutionFeature.Config.class),
            Map.entry(FeatureDefinition.SCHEMATIC_DRAG_DROP, SchematicDragDropFeature.Config.class));

    private FeatureSections() {}

    /** @return the section class, or null when the feature has no settings beyond its switch. */
    public static Class<?> sectionType(FeatureDefinition definition) {
        return SECTIONS.get(definition);
    }
}
