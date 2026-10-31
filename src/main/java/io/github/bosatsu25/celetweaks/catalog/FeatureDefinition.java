package io.github.bosatsu25.celetweaks.catalog;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Objects;

/**
 * The single source of truth for every feature switch. Config keys, translation keys,
 * settings rows and documentation are all derived from this enum.
 */
public enum FeatureDefinition {
    // --- Core: validates the HUD / mixin pipeline on every version ---------------------------
    SIDEBAR_LAYOUT("sidebar_layout", FeatureCategory.HUD, RiskKind.VISUAL, Tier.CORE, true),
    GREY_TEAMS("grey_teams", FeatureCategory.HUD, RiskKind.VISUAL, Tier.CORE, true),

    // --- Vanilla only ------------------------------------------------------------------------
    GREY_CHAT("grey_chat", FeatureCategory.HUD, RiskKind.VISUAL, Tier.VANILLA, true),
    PORTAL_FRAME_PROTECT("portal_frame_protect", FeatureCategory.GUARD, RiskKind.GUARD, Tier.VANILLA, true),
    PORTAL_DOORWAY_CLEAR("portal_doorway_clear", FeatureCategory.GUARD, RiskKind.GUARD, Tier.VANILLA, true),
    NO_DIG_UNDERFOOT("no_dig_underfoot", FeatureCategory.GUARD, RiskKind.GUARD, Tier.VANILLA, true),
    ITEM_USE_GUARD("item_use_guard", FeatureCategory.GUARD, RiskKind.GUARD, Tier.VANILLA, true),
    HOTBAR_LOCK("hotbar_lock", FeatureCategory.GUARD, RiskKind.GUARD, Tier.VANILLA, true),
    SLOW_LOOK("slow_look", FeatureCategory.UTILITY, RiskKind.VISUAL, Tier.VANILLA, true),
    TEXT_NOTES("text_notes", FeatureCategory.UTILITY, RiskKind.VISUAL, Tier.VANILLA, true),
    ACTION_LOG("action_log", FeatureCategory.UTILITY, RiskKind.VISUAL, Tier.VANILLA, true),
    RANGE_OVERLAY("range_overlay", FeatureCategory.VIEW, RiskKind.VISUAL, Tier.VANILLA, true),
    ENTITY_FILTER("entity_filter", FeatureCategory.VIEW, RiskKind.VISUAL, Tier.VANILLA, true),
    BLOCK_FILTER("block_filter", FeatureCategory.VIEW, RiskKind.VISUAL, Tier.VANILLA, true),
    LAVA_SOURCE_TINT("lava_source_tint", FeatureCategory.VIEW, RiskKind.VISUAL, Tier.VANILLA, true),

    // --- Optional external mods (no-op when absent) -----------------------------------------
    SHIFT_TO_MASS_CRAFT("shift_to_mass_craft", FeatureCategory.GUARD, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.ITEMSCROLLER),
    SYNCMATICA_REMOVE_LOCK("syncmatica_remove_lock", FeatureCategory.GUARD, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.SYNCMATICA),
    TOOL_SWITCH_SCOPE("tool_switch_scope", FeatureCategory.MOD_LINK, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.TWEAKEROO),
    TWEAKEROO_RESYNC("tweakeroo_resync", FeatureCategory.MOD_LINK, RiskKind.VISUAL, Tier.INTEGRATION, true, Mods.TWEAKEROO),
    AUTO_PICK_SCOPE("auto_pick_scope", FeatureCategory.MOD_LINK, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.TWEAKERMORE),
    SYNCER_TUNE("syncer_tune", FeatureCategory.MOD_LINK, RiskKind.VISUAL, Tier.INTEGRATION, true, Mods.TWEAKERMORE),
    PLACEMENT_CONVERSIONS("placement_conversions", FeatureCategory.MOD_LINK, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.LITEMATICA, Mods.TWEAKERMORE),
    MATERIAL_SUBSTITUTION("material_substitution", FeatureCategory.MOD_LINK, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.LITEMATICA),
    MATERIAL_LIST_RECOUNT("material_list_recount", FeatureCategory.MOD_LINK, RiskKind.VISUAL, Tier.INTEGRATION, true, Mods.LITEMATICA),
    SCHEMATIC_DRAG_DROP("schematic_drag_drop", FeatureCategory.MOD_LINK, RiskKind.GUARD, Tier.INTEGRATION, true, Mods.LITEMATICA);

    public static final List<FeatureDefinition> VALUES = List.of(values());

    private final String id;
    private final FeatureCategory category;
    private final RiskKind risk;
    private final Tier tier;
    private final boolean implemented;
    private final List<String> requiredMods;

    FeatureDefinition(String id, FeatureCategory category, RiskKind risk, Tier tier, boolean implemented, String... requiredMods) {
        this.id = Objects.requireNonNull(id);
        this.category = Objects.requireNonNull(category);
        this.risk = Objects.requireNonNull(risk);
        this.tier = Objects.requireNonNull(tier);
        this.implemented = implemented;
        this.requiredMods = List.of(requiredMods);
    }

    public String id() { return id; }
    public FeatureCategory category() { return category; }
    public RiskKind risk() { return risk; }
    public Tier tier() { return tier; }
    /** False for catalogued features whose runtime has not been written yet; shown greyed out. */
    public boolean implemented() { return implemented; }
    public List<String> requiredMods() { return requiredMods; }

    public String nameKey() { return "celetweaks.feature." + id; }
    public String descriptionKey() { return "celetweaks.feature." + id + ".desc"; }

    /** True when the feature can run in this client: implemented and every required mod is loaded. */
    public boolean isAvailable() {
        if (!implemented) return false;
        FabricLoader loader = FabricLoader.getInstance();
        for (String mod : requiredMods) {
            if (!loader.isModLoaded(mod)) return false;
        }
        return true;
    }

    /** Mod ids of optional integrations. */
    public static final class Mods {
        public static final String MALILIB = "malilib";
        public static final String LITEMATICA = "litematica";
        public static final String TWEAKEROO = "tweakeroo";
        public static final String TWEAKERMORE = "tweakermore";
        public static final String SYNCMATICA = "syncmatica";
        public static final String ITEMSCROLLER = "itemscroller";
        public static final String SODIUM = "sodium";
        private Mods() {}
    }
}
