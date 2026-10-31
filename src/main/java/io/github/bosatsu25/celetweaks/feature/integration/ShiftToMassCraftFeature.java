package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Stops Item Scroller's mass-craft keybinds from firing in the plain player inventory unless Shift is held. */
public final class ShiftToMassCraftFeature extends Feature {
    public ShiftToMassCraftFeature() {
        super(FeatureDefinition.SHIFT_TO_MASS_CRAFT);
    }

    public static boolean shouldBlock() {
        if (!CeleConfig.get().isEnabled(FeatureDefinition.SHIFT_TO_MASS_CRAFT)) return false;
        Screen screen = Compat.currentScreen();
        return screen instanceof InventoryScreen && !Minecraft.getInstance().hasShiftDown();
    }
}
