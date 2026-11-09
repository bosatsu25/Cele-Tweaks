package io.github.bosatsu25.celetweaks.feature.integration;

/**
 * The two-state switch behind Syncer Tune. A load sample at or above the enter threshold moves it
 * to RELAXED; only a sample below the (lower) exit threshold moves it back, so a load hovering
 * around one threshold does not flip the syncer settings every sample.
 * <p>
 * Thresholds are sanitised on every sample: enter is at least 1, exit is at least 0 and never above
 * enter. The caller owns the side effects; this class only says when they are due.
 */
final class LoadHysteresis {
    /** What the caller has to do after a sample. */
    enum Change { NONE, RELAX, RESTORE }

    private boolean relaxed;

    boolean isRelaxed() {
        return relaxed;
    }

    /** Feeds one load sample and returns the side effect it calls for. */
    Change sample(int load, int enterThreshold, int exitThreshold) {
        int enter = Math.max(1, enterThreshold);
        int exit = Math.min(enter, Math.max(0, exitThreshold));
        if (!relaxed && load >= enter) {
            relaxed = true;
            return Change.RELAX;
        }
        if (relaxed && load < exit) {
            relaxed = false;
            return Change.RESTORE;
        }
        return Change.NONE;
    }

    /** Forces the normal state (feature disabled, world left, relaxing failed); RESTORE when it was relaxed. */
    Change reset() {
        if (!relaxed) return Change.NONE;
        relaxed = false;
        return Change.RESTORE;
    }
}
