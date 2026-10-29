package io.github.bosatsu25.celetweaks.catalog;

public enum Tier {
    /** Shipped with the runtime foundation; validates the HUD / mixin pipeline across versions. */
    CORE,
    /** Vanilla-only features. */
    VANILLA,
    /** Guards, redirects and refreshes layered on optional external mods. */
    INTEGRATION
}
