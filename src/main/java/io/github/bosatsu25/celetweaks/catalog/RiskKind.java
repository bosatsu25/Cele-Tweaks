package io.github.bosatsu25.celetweaks.catalog;

/**
 * What a feature is allowed to do. There is deliberately no AUTOMATION kind:
 * Cele-Tweaks never performs an action on the player's behalf.
 */
public enum RiskKind {
    /** Changes only what the local client draws. */
    VISUAL,
    /** Cancels an action the player (or another mod) was about to perform. Never adds one. */
    GUARD
}
