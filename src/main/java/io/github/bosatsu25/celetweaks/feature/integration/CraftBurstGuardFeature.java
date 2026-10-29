package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Stops Item Scroller's mass-craft keybinds from firing in the plain player inventory unless Shift is held. */
public final class CraftBurstGuardFeature extends Feature {
    public CraftBurstGuardFeature() {
        super(FeatureDefinition.CRAFT_BURST_GUARD);
    }

    public static boolean shouldBlock() {
        if (!CeleConfig.get().isEnabled(FeatureDefinition.CRAFT_BURST_GUARD)) return false;
        Screen screen = Compat.currentScreen();
        return screen instanceof InventoryScreen && !Minecraft.getInstance().hasShiftDown();
    }
}
