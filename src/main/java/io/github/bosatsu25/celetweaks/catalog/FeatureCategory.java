package io.github.bosatsu25.celetweaks.catalog;

/** User-facing grouping. Mirrors the tabs of the settings screen. */
public enum FeatureCategory {
    SAFETY,
    BUILDING,
    FOCUS,
    INVENTORY,
    RENDERING,
    COMPATIBILITY;

    public String nameKey() { return "celetweaks.category." + name().toLowerCase(); }
}
