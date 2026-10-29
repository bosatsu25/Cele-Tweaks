package io.github.bosatsu25.celetweaks.gui;

import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.hotkey.KeyCombo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Captures a key combination: hold the keys, release any one of them to commit.
 * Escape clears the binding. Key events come through {@link HotkeyManager}, not Screen callbacks,
 * so this screen does not depend on the vanilla key-event signature.
 */
public final class HotkeyCaptureScreen extends Screen implements HotkeyManager.KeyCaptureListener {
    private final Screen parent;
    private final String actionId;
    private final Set<Integer> captured = new HashSet<>();
    private boolean done;

    public HotkeyCaptureScreen(Screen parent, String actionId) {
        super(Component.translatable("celetweaks.screen.hotkey_title"));
        this.parent = parent;
        this.actionId = actionId;
    }

    @Override
    protected void init() {
        HotkeyManager.setCaptureListener(this);
    }

    @Override
    public void onPress(int key, Set<Integer> pressed) {
        if (done) return;
        if (key == InputConstants.KEY_ESCAPE) {
            HotkeyManager.bind(actionId, KeyCombo.NONE);
            finish();
            return;
        }
        captured.addAll(pressed);
    }

    @Override
    public void onRelease(Set<Integer> stillPressed) {
        if (done || captured.isEmpty()) return;
        HotkeyManager.bind(actionId, KeyCombo.of(captured));
        finish();
    }

    private void finish() {
        done = true;
        HotkeyManager.setCaptureListener(null);
        HotkeyManager.resetPressed();
        CeleScreens.open(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        Gfx.centeredText(graphics, font, title, width / 2, height / 2 - 30, 0xFFFFFFFF);
        Gfx.centeredText(graphics, font, Component.translatable("celetweaks.screen.hotkey_hint"), width / 2, height / 2 - 10, 0xFFA0A0A0);
        Gfx.centeredText(graphics, font, KeyCombo.of(captured).displayName(), width / 2, height / 2 + 10, 0xFFFFFF55);
    }

    @Override
    public void removed() {
        HotkeyManager.setCaptureListener(null);
    }
}
