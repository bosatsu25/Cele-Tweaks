package io.github.bosatsu25.celetweaks.catalog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checklist over {@link FeatureDefinition}, the single source of truth for every feature: the things
 * a person forgets when adding a feature (a translation, a mod requirement, a stray capital letter)
 * and that nothing else would catch until the settings screen shows a raw key.
 */
class FeatureCatalogTest {
    private static final List<String> LANGUAGES = List.of("en_us", "ja_jp");

    @Test
    void theCatalogHasTheDocumentedNumberOfFeatures() {
        assertEquals(25, FeatureDefinition.VALUES.size(), "README and FEATURE_CATALOG say 25 features; update them with this number");
    }

    @Test
    void idsAreUniqueLowerCaseSnakeCase() {
        Set<String> seen = new HashSet<>();
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            assertTrue(definition.id().matches("[a-z][a-z0-9_]*"), definition + " has id " + definition.id());
            assertTrue(seen.add(definition.id()), "duplicate id " + definition.id());
        }
    }

    @Test
    void integrationFeaturesNameTheirModsAndOthersNameNone() {
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            boolean integration = definition.tier() == Tier.INTEGRATION;
            assertEquals(integration, !definition.requiredMods().isEmpty(),
                    definition + " is tier " + definition.tier() + " and requires " + definition.requiredMods());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"en_us", "ja_jp"})
    void everyFeatureAndCategoryIsTranslated(String language) throws IOException {
        JsonObject lang = lang(language);
        List<String> missing = new ArrayList<>();
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            for (String key : List.of(definition.nameKey(), definition.descriptionKey())) {
                if (!lang.has(key)) missing.add(key);
            }
        }
        for (FeatureCategory category : FeatureCategory.values()) {
            if (!lang.has(category.nameKey())) missing.add(category.nameKey());
        }
        assertTrue(missing.isEmpty(), language + " lacks " + missing);
    }

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        Set<String> english = lang(LANGUAGES.get(0)).keySet();
        Set<String> japanese = lang(LANGUAGES.get(1)).keySet();
        Set<String> onlyEnglish = new HashSet<>(english);
        onlyEnglish.removeAll(japanese);
        Set<String> onlyJapanese = new HashSet<>(japanese);
        onlyJapanese.removeAll(english);
        assertTrue(onlyEnglish.isEmpty() && onlyJapanese.isEmpty(),
                "only in en_us: " + onlyEnglish + "; only in ja_jp: " + onlyJapanese);
    }

    private static JsonObject lang(String language) throws IOException {
        String path = "/assets/celetweaks/lang/" + language + ".json";
        try (InputStream in = FeatureCatalogTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
