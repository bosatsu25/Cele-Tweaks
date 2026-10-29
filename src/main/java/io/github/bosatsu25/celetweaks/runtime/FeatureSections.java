package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.feature.aim.PrecisionAimFeature;
import io.github.bosatsu25.celetweaks.feature.blockfilter.BlockFilterFeature;
import io.github.bosatsu25.celetweaks.feature.entityfilter.EntityFilterFeature;
import io.github.bosatsu25.celetweaks.feature.guard.FootingGuardFeature;
import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.AutoPickScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.MaterialSubstitutionFeature;
import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SchematicDropFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SharedPlanLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncerTuneFeature;
import io.github.bosatsu25.celetweaks.feature.integration.ToolSwitchScopeFeature;
import io.github.bosatsu25.celetweaks.feature.integration.TweakerooResyncFeature;
import io.github.bosatsu25.celetweaks.feature.lava.LavaHighlightFeature;
import io.github.bosatsu25.celetweaks.feature.log.ActionLogFeature;
import io.github.bosatsu25.celetweaks.feature.range.RangeOverlayFeature;
import io.github.bosatsu25.celetweaks.feature.scoreboard.ScoreboardPanelFeature;

import java.util.Map;

/** Maps a feature switch to the config section class the settings screen edits. */
public final class FeatureSections {
    private static final Map<FeatureDefinition, Class<?>> SECTIONS = Map.ofEntries(
            Map.entry(FeatureDefinition.SCOREBOARD_PANEL, ScoreboardPanelFeature.Config.class),
            Map.entry(FeatureDefinition.FOOTING_GUARD, FootingGuardFeature.Config.class),
            Map.entry(FeatureDefinition.ITEM_USE_GUARD, ItemUseGuardFeature.Config.class),
            Map.entry(FeatureDefinition.HOTBAR_LOCK, HotbarLockFeature.Config.class),
            Map.entry(FeatureDefinition.PRECISION_AIM, PrecisionAimFeature.Config.class),
            Map.entry(FeatureDefinition.ACTION_LOG, ActionLogFeature.Config.class),
            Map.entry(FeatureDefinition.RANGE_OVERLAY, RangeOverlayFeature.Config.class),
            Map.entry(FeatureDefinition.ENTITY_FILTER, EntityFilterFeature.Config.class),
            Map.entry(FeatureDefinition.BLOCK_FILTER, BlockFilterFeature.Config.class),
            Map.entry(FeatureDefinition.LAVA_HIGHLIGHT, LavaHighlightFeature.Config.class),
            Map.entry(FeatureDefinition.SHARED_PLAN_LOCK, SharedPlanLockFeature.Config.class),
            Map.entry(FeatureDefinition.TOOL_SWITCH_SCOPE, ToolSwitchScopeFeature.Config.class),
            Map.entry(FeatureDefinition.TWEAKEROO_RESYNC, TweakerooResyncFeature.Config.class),
            Map.entry(FeatureDefinition.AUTO_PICK_SCOPE, AutoPickScopeFeature.Config.class),
            Map.entry(FeatureDefinition.SYNCER_TUNE, SyncerTuneFeature.Config.class),
            Map.entry(FeatureDefinition.PLACEMENT_CONVERSIONS, PlacementConversionsFeature.Config.class),
            Map.entry(FeatureDefinition.MATERIAL_SUBSTITUTION, MaterialSubstitutionFeature.Config.class),
            Map.entry(FeatureDefinition.SCHEMATIC_DROP, SchematicDropFeature.Config.class));

    private FeatureSections() {}

    /** @return the section class, or null when the feature has no settings beyond its switch. */
    public static Class<?> sectionType(FeatureDefinition definition) {
        return SECTIONS.get(definition);
    }
}
