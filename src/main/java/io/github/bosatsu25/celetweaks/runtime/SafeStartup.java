package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;

import java.util.ArrayList;
import java.util.List;

/** Runs a startup stage and records, rather than propagates, its failure. */
public final class SafeStartup {
    private static final List<String> FAILURES = new ArrayList<>();

    private SafeStartup() {}

    public static void run(String stage, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException | LinkageError failure) {
            FAILURES.add(stage);
            CeleTweaksClient.LOGGER.error("Startup stage '{}' failed and was skipped", stage, failure);
        }
    }

    public static List<String> failures() {
        return List.copyOf(FAILURES);
    }
}
