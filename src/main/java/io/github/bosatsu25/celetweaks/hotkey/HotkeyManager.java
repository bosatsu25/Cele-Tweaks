package io.github.bosatsu25.celetweaks.hotkey;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.JsonFiles;
import io.github.bosatsu25.celetweaks.gui.CeleScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.bosatsu25.celetweaks.compat.Compat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Key-combination hotkeys kept out of the vanilla Controls screen so that every feature can have one.
 * Key events arrive from {@code KeyboardHandlerMixin}; combos fire on the press that completes them,
 * only while no screen is open. Bindings persist in {@code config/celetweaks/hotkeys.json}.
 */
public final class HotkeyManager {
    public static final String OPEN_SETTINGS = "open_settings";

    private static final Map<String, HotkeyAction> ACTIONS = new LinkedHashMap<>();
    private static final Map<String, KeyCombo> BINDINGS = new HashMap<>();
    private static final Set<Integer> PRESSED = new HashSet<>();
    private static Path file;
    private static KeyCaptureListener capture;

    private HotkeyManager() {}

    public static void init() {
        file = CeleConfig.get().directory().resolve("hotkeys.json");
        register(OPEN_SETTINGS, Component.translatable("celetweaks.hotkey.open_settings"), CeleScreens::openSettingsNextTick);
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            register(toggleId(definition),
                    Component.translatable("celetweaks.hotkey.toggle", Component.translatable(definition.nameKey())),
                    () -> toggleFromHotkey(definition));
        }
        load();
    }

    public static String toggleId(FeatureDefinition definition) { return "toggle:" + definition.id(); }

    public static void register(String id, Component name, Runnable run) {
        ACTIONS.put(id, new HotkeyAction(id, name, run));
    }

    public static List<HotkeyAction> actions() { return new ArrayList<>(ACTIONS.values()); }

    public static KeyCombo binding(String actionId) { return BINDINGS.getOrDefault(actionId, KeyCombo.NONE); }

    public static void bind(String actionId, KeyCombo combo) {
        if (combo == null || combo.isEmpty()) BINDINGS.remove(actionId);
        else BINDINGS.put(actionId, combo);
        save();
    }

    /** While set, key events are routed to the listener instead of firing actions. */
    public static void setCaptureListener(KeyCaptureListener listener) { capture = listener; }

    /** Entry point from the keyboard mixin. */
    public static void onKey(int key, int action) {
        if (key == InputConstants.UNKNOWN.getValue()) return;
        if (action == InputConstants.RELEASE) {
            PRESSED.remove(key);
            if (capture != null) capture.onRelease(Set.copyOf(PRESSED));
            return;
        }
        if (action != InputConstants.PRESS) return;
        PRESSED.add(key);
        if (capture != null) {
            capture.onPress(key, Set.copyOf(PRESSED));
            return;
        }
        if (Compat.currentScreen() != null) return;
        for (Map.Entry<String, KeyCombo> entry : BINDINGS.entrySet()) {
            if (entry.getValue().matches(PRESSED)) {
                HotkeyAction target = ACTIONS.get(entry.getKey());
                if (target != null) {
                    try {
                        target.run().run();
                    } catch (RuntimeException | LinkageError failure) {
                        CeleTweaksClient.LOGGER.error("Hotkey action '{}' failed", entry.getKey(), failure);
                    }
                }
            }
        }
    }

    /** True while every key of the action's combination is held and no screen is open. */
    public static boolean isHeld(String actionId) {
        KeyCombo combo = BINDINGS.get(actionId);
        if (combo == null || combo.isEmpty() || Compat.currentScreen() != null) return false;
        for (int key : combo.keys()) {
            if (!PRESSED.contains(key)) return false;
        }
        return true;
    }

    /** Clears the pressed set, e.g. when focus is lost. */
    public static void resetPressed() { PRESSED.clear(); }

    private static void toggleFromHotkey(FeatureDefinition definition) {
        if (!definition.isAvailable()) return;
        CeleConfig.get().toggle(definition);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            boolean on = CeleConfig.get().isSwitchedOn(definition);
            client.player.sendOverlayMessage(Component.translatable(
                    on ? "celetweaks.toggle.on" : "celetweaks.toggle.off",
                    Component.translatable(definition.nameKey())));
        }
    }

    private static void load() {
        BINDINGS.clear();
        JsonObject root = JsonFiles.readObject(file);
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonArray()) continue;
            List<Integer> keys = new ArrayList<>();
            boolean complete = true;
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) { complete = false; break; }
                int code = Compat.keyCode(element.getAsString());
                if (code < 0) { complete = false; break; }
                keys.add(code);
            }
            // A combo with a key this version does not know is dropped rather than half-bound.
            if (complete && !keys.isEmpty()) BINDINGS.put(entry.getKey(), KeyCombo.of(keys));
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, KeyCombo> entry : BINDINGS.entrySet()) {
            JsonArray keys = new JsonArray();
            for (int key : entry.getValue().keys()) keys.add(Compat.keyId(key));
            root.add(entry.getKey(), keys);
        }
        JsonFiles.writeObject(file, root);
    }

    public interface KeyCaptureListener {
        void onPress(int key, Set<Integer> pressed);
        void onRelease(Set<Integer> stillPressed);
    }
}
