package io.github.bosatsu25.celetweaks.feature.notes;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.gui.CeleScreens;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Small local text notes (coordinates, build order, to-dos). Stored in config/celetweaks/notes.txt, never sent anywhere. */
public final class TextNotesFeature extends Feature {
    public static final String OPEN_ACTION = "text_notes.open";
    public static final int MAX_LINES = 64;
    public static final int MAX_LINE_LENGTH = 256;

    public TextNotesFeature() {
        super(FeatureDefinition.TEXT_NOTES);
    }

    @Override
    public void init() {
        HotkeyManager.register(OPEN_ACTION, Component.translatable("celetweaks.hotkey.text_notes"), TextNotesFeature::openIfEnabled);
    }

    public static void openIfEnabled() {
        if (!CeleConfig.get().isEnabled(FeatureDefinition.TEXT_NOTES)) return;
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> CeleScreens.open(new TextNotesScreen(io.github.bosatsu25.celetweaks.compat.Compat.currentScreen())));
    }

    static Path file() {
        return CeleConfig.get().directory().resolve("notes.txt");
    }

    static List<String> load() {
        List<String> lines = new ArrayList<>();
        Path path = file();
        if (Files.isRegularFile(path)) {
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    if (lines.size() >= MAX_LINES) break;
                    lines.add(line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) : line);
                }
            } catch (IOException | RuntimeException failure) {
                CeleTweaksClient.LOGGER.warn("Could not read text notes", failure);
            }
        }
        while (lines.size() < MAX_LINES) lines.add("");
        return lines;
    }

    static void save(List<String> lines) {
        int last = lines.size() - 1;
        while (last >= 0 && lines.get(last).isEmpty()) last--;
        String text = String.join("\n", lines.subList(0, last + 1));
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling("notes.txt.tmp");
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            CeleTweaksClient.LOGGER.error("Could not save text notes", failure);
        }
    }
}
