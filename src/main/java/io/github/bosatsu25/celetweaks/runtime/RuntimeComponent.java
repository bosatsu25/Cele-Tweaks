package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import net.minecraft.client.Minecraft;

import java.util.Set;

/** A unit the {@link FeatureManager} initializes, ticks and quarantines on failure. */
public interface RuntimeComponent {
    String id();

    /** Feature switches this component serves. Used to route toggle notifications. */
    default Set<FeatureDefinition> definitions() { return Set.of(); }

    default void init() {}

    /** Called once per client tick (only while not quarantined). */
    default void tick(Minecraft client) {}

    /** Called when the client level changes (join, disconnect, dimension change). Drop session state here. */
    default void resetSession(Minecraft client) {}

    /** Called when one of {@link #definitions()} is switched on or off from the UI or a hotkey. */
    default void onToggle(FeatureDefinition definition, boolean enabled) {}

    /** Called after the component has been quarantined; release anything the component changed in the client. */
    default void onQuarantined(Minecraft client) {}
}
