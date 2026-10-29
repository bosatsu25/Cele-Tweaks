package io.github.bosatsu25.celetweaks.gui;

import io.github.bosatsu25.celetweaks.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Single place that opens screens, so a rename of the vanilla API is fixed once. */
public final class CeleScreens {
    private CeleScreens() {}

    public static void open(Screen screen) {
        Minecraft.getInstance().setScreenAndShow(screen);
    }

    public static void openSettingsNextTick() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> open(new FeatureListScreen(Compat.currentScreen())));
    }
}
