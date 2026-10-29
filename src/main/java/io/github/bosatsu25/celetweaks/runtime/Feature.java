package io.github.bosatsu25.celetweaks.runtime;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;

import java.util.Objects;
import java.util.Set;

/** Convenience base for a component that serves exactly one feature switch. */
public abstract class Feature implements RuntimeComponent {
    protected final FeatureDefinition definition;

    protected Feature(FeatureDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    @Override
    public final String id() { return definition.id(); }

    @Override
    public final Set<FeatureDefinition> definitions() { return Set.of(definition); }

    /** Enabled in config and every required mod is present. */
    public final boolean isEnabled() {
        return CeleConfig.get().isEnabled(definition);
    }

    protected final <T> T config(Class<T> type) {
        return CeleConfig.get().section(definition, type);
    }
}
