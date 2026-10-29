package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * After joining a world, re-runs Tweakeroo's own change callback for the listed Tweakeroo toggles that
 * are switched on, so client-side state they apply (gamma override, for example) is re-established.
 * Cele never changes a Tweakeroo value; reflection keeps Tweakeroo optional.
 */
public final class TweakerooResyncFeature extends Feature {
    public TweakerooResyncFeature() {
        super(FeatureDefinition.TWEAKEROO_RESYNC);
    }

    @Override
    public void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(this::resync));
    }

    private void resync() {
        if (!isEnabled()) return;
        Class<?> toggles;
        try {
            toggles = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
        } catch (ReflectiveOperationException | LinkageError failure) {
            return;
        }
        for (String name : config(Config.class).toggles) {
            try {
                Object toggle = toggles.getField(name.trim()).get(null);
                if (Boolean.TRUE.equals(toggle.getClass().getMethod("getBooleanValue").invoke(toggle))) {
                    toggle.getClass().getMethod("onValueChanged").invoke(toggle);
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                CeleTweaksClient.LOGGER.debug("Tweakeroo resync skipped {}: {}", name, failure.toString());
            }
        }
    }

    public static final class Config {
        /** Tweakeroo FeatureToggle constant names. */
        public List<String> toggles = new ArrayList<>(List.of("TWEAK_GAMMA_OVERRIDE"));
    }
}
