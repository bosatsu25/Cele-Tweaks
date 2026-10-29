package io.github.bosatsu25.celetweaks.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code config/celetweaks/features.json}:
 * <pre>{ "enabled": { "<feature id>": bool }, "sections": { "<feature id>": { ...feature config... } } }</pre>
 * Each feature owns its own section POJO; this class only knows section names.
 */
public final class CeleConfig {
    private static final CeleConfig INSTANCE = new CeleConfig();

    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve(CeleTweaksClient.MOD_ID);
    private final Path featuresFile = dir.resolve("features.json");

    private final Map<FeatureDefinition, Boolean> enabled = new EnumMap<>(FeatureDefinition.class);
    private final Map<String, Object> sections = new HashMap<>();
    private JsonObject rawSections = new JsonObject();
    private JsonObject rawRoot = new JsonObject();

    private CeleConfig() {}

    public static CeleConfig get() { return INSTANCE; }

    public Path directory() { return dir; }

    public synchronized void load() {
        rawRoot = JsonFiles.readObject(featuresFile);
        enabled.clear();
        sections.clear();
        JsonObject enabledObject = child(rawRoot, "enabled");
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            JsonElement value = enabledObject.get(definition.id());
            enabled.put(definition, value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                    && value.getAsBoolean());
        }
        rawSections = child(rawRoot, "sections");
    }

    public synchronized void save() {
        JsonObject enabledObject = new JsonObject();
        for (Map.Entry<FeatureDefinition, Boolean> entry : enabled.entrySet()) {
            enabledObject.addProperty(entry.getKey().id(), entry.getValue());
        }
        rawRoot.add("enabled", enabledObject);
        for (Map.Entry<String, Object> entry : sections.entrySet()) {
            rawSections.add(entry.getKey(), JsonFiles.GSON.toJsonTree(entry.getValue()));
        }
        rawRoot.add("sections", rawSections);
        JsonFiles.writeObject(featuresFile, rawRoot);
    }

    /** Enabled by the user and available in this client. */
    public boolean isEnabled(FeatureDefinition definition) {
        return isSwitchedOn(definition) && definition.isAvailable() && !FeatureManager.get().isQuarantined(definition);
    }

    /** The stored switch, regardless of availability. */
    public boolean isSwitchedOn(FeatureDefinition definition) {
        return enabled.getOrDefault(definition, Boolean.FALSE);
    }

    public void setEnabled(FeatureDefinition definition, boolean value) {
        boolean previous;
        synchronized (this) {
            previous = isSwitchedOn(definition);
            enabled.put(definition, value);
        }
        if (previous != value) {
            save();
            FeatureManager.get().notifyToggle(definition, value && definition.isAvailable());
        }
    }

    public void toggle(FeatureDefinition definition) {
        setEnabled(definition, !isSwitchedOn(definition));
    }

    /** Returns the feature's config section, deserialized once and cached. Missing fields keep their defaults. */
    @SuppressWarnings("unchecked")
    public synchronized <T> T section(FeatureDefinition definition, Class<T> type) {
        Object cached = sections.get(definition.id());
        if (type.isInstance(cached)) return (T) cached;
        T value = null;
        JsonElement raw = rawSections.get(definition.id());
        if (raw != null && raw.isJsonObject()) {
            try {
                value = JsonFiles.GSON.fromJson(raw, type);
            } catch (RuntimeException failure) {
                CeleTweaksClient.LOGGER.warn("Config section '{}' is malformed; using defaults", definition.id());
            }
        }
        if (value == null) {
            try {
                value = type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Config section type needs a public no-arg constructor: " + type, failure);
            }
        }
        sections.put(definition.id(), value);
        return value;
    }

    private static JsonObject child(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        if (element != null && element.isJsonObject()) return element.getAsJsonObject();
        JsonObject fresh = new JsonObject();
        parent.add(key, fresh);
        return fresh;
    }
}
