package io.github.bosatsu25.celetweaks.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Text drawing used by Cele screens and HUD. Kept in one place because the 26.x names moved. */
public final class Gfx {
    private Gfx() {}

    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    public static void centeredText(GuiGraphicsExtractor graphics, Font font, Component text, int centerX, int y, int color) {
        graphics.centeredText(font, text, centerX, y, color);
    }
}
