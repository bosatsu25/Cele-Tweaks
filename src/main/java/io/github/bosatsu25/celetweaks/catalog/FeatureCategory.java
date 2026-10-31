package io.github.bosatsu25.celetweaks.catalog;

/** User-facing grouping. Mirrors the tabs of the settings screen. */
public enum FeatureCategory {
    /** Stops an action the player (or another mod) was about to take. */
    GUARD,
    /** Helpers the player calls up: notes, logs, slow look. */
    UTILITY,
    /** Changes to the screen overlay: sidebar, team and chat colours. */
    HUD,
    /** Changes to how the world itself is drawn. */
    VIEW,
    /** Adjustments to optional external mods. */
    MOD_LINK;

    public String nameKey() { return "celetweaks.category." + name().toLowerCase(); }
}
