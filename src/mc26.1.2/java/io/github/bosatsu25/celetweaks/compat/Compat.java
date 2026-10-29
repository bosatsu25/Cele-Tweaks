package io.github.bosatsu25.celetweaks.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minecraft 26.1.2 specifics. Every supported version provides this class with the same API. */
public final class Compat {
    private Compat() {}

    /** The screen currently shown, or null in game. */
    public static Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    /** Re-mesh every loaded chunk (used when the block filter changes). */
    public static void reloadChunks(Minecraft client) {
        if (client.levelRenderer != null) client.levelRenderer.allChanged();
    }

    /**
     * Stable name of a keyboard key ("key.keyboard.k"). Stored in hotkeys.json instead of the
     * numeric code, because 26.3 moved input from GLFW to SDL and the numbers changed.
     */
    public static String keyId(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getName();
    }

    /** Key code for a stored name, or -1 when the name is unknown on this version. */
    public static int keyCode(String id) {
        try {
            InputConstants.Key key = InputConstants.getKey(id);
            return key == InputConstants.UNKNOWN ? -1 : key.getValue();
        } catch (IllegalArgumentException unknown) {
            return -1;
        }
    }

    /** Display name of a keyboard key code. */
    public static Component keyName(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName();
    }
}
