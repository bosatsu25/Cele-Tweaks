package io.github.bosatsu25.celetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bosatsu25.celetweaks.CeleTweaksClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Fail-soft JSON persistence: a broken file is moved aside and replaced by defaults,
 * writes are atomic, and unknown keys are preserved by callers operating on the JsonObject.
 */
public final class JsonFiles {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonFiles() {}

    public static JsonObject readObject(Path path) {
        if (!Files.isRegularFile(path)) return new JsonObject();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(text);
            if (element.isJsonObject()) return element.getAsJsonObject();
            throw new IllegalStateException("root is not an object");
        } catch (IOException | RuntimeException failure) {
            Path broken = path.resolveSibling(path.getFileName() + ".broken-" + Instant.now().getEpochSecond());
            CeleTweaksClient.LOGGER.error("Config {} is unreadable ({}); moving it to {} and using defaults",
                    path.getFileName(), failure.getClass().getSimpleName(), broken.getFileName());
            try {
                Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                CeleTweaksClient.LOGGER.warn("Could not move broken config aside", moveFailure);
            }
            return new JsonObject();
        }
    }

    public static void writeObject(Path path, JsonObject object) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(object), StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            CeleTweaksClient.LOGGER.error("Could not write {}", path.getFileName(), failure);
        }
    }
}
