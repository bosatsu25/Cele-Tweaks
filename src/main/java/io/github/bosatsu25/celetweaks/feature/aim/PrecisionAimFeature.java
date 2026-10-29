package io.github.bosatsu25.celetweaks.feature.aim;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Hold-to-use precision aiming: while its key combination is held, mouse sensitivity is scaled by a
 * factor of the player's own setting; releasing the keys, leaving the world or disabling the feature
 * puts the original value back.
 */
public final class PrecisionAimFeature extends Feature {
    public static final String HOLD_ACTION = "precision_aim.hold";

    private Double original;

    public PrecisionAimFeature() {
        super(FeatureDefinition.PRECISION_AIM);
    }

    @Override
    public void init() {
        HotkeyManager.register(HOLD_ACTION, Component.translatable("celetweaks.hotkey.precision_aim.hold"), () -> {});
    }

    @Override
    public void tick(Minecraft client) {
        boolean wanted = isEnabled() && client.player != null && HotkeyManager.isHeld(HOLD_ACTION);
        if (wanted && original == null) {
            original = client.options.sensitivity().get();
            double factor = Math.max(1, Math.min(100, config(Config.class).factorPercent)) / 100.0;
            client.options.sensitivity().set(Math.max(0.0, Math.min(1.0, original * factor)));
        } else if (!wanted) {
            restore(client);
        }
    }

    @Override
    public void resetSession(Minecraft client) {
        restore(client);
    }

    @Override
    public void onQuarantined(Minecraft client) {
        restore(client);
    }

    private void restore(Minecraft client) {
        if (original != null) {
            client.options.sensitivity().set(original);
            original = null;
        }
    }

    public static final class Config {
        /** Sensitivity while held, as a percentage of the player's normal setting. */
        @Range(min = 1, max = 100) public int factorPercent = 35;
    }
}
