package io.github.bosatsu25.celetweaks.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JsonFiles}: the fail-soft contract of the config files. Partitions of what is on disk:
 * no file, a JSON object, valid JSON that is not an object, text that is not JSON. The broken ones
 * must be moved aside (never deleted, never left in place) and replaced by defaults.
 */
class JsonFilesTest {
    @TempDir
    Path dir;

    @Test
    void aMissingFileReadsAsAnEmptyObjectAndCreatesNothing() throws IOException {
        Path file = dir.resolve("features.json");
        JsonObject read = JsonFiles.readObject(file);
        assertEquals(0, read.size());
        assertFalse(Files.exists(file));
    }

    @Test
    void anObjectReadsBackAsWritten() throws IOException {
        Path file = dir.resolve("features.json");
        Files.writeString(file, "{\"enabled\": {\"hotbar_lock\": true}, \"unknown\": 1}");
        JsonObject read = JsonFiles.readObject(file);
        assertTrue(read.getAsJsonObject("enabled").get("hotbar_lock").getAsBoolean());
        assertEquals(1, read.get("unknown").getAsInt(), "unknown keys survive");
    }

    @Test
    void validJsonThatIsNotAnObjectIsMovedAside() throws IOException {
        assertMovedAside("[1, 2, 3]");
    }

    @Test
    void textThatIsNotJsonIsMovedAside() throws IOException {
        assertMovedAside("{ this is not json");
    }

    @Test
    void writeCreatesParentDirectoriesAndReplacesTheOldContent() throws IOException {
        Path file = dir.resolve("deep/er/features.json");
        JsonObject first = new JsonObject();
        first.addProperty("a", 1);
        JsonFiles.writeObject(file, first);
        JsonObject second = new JsonObject();
        second.addProperty("b", 2);
        JsonFiles.writeObject(file, second);

        JsonObject read = JsonFiles.readObject(file);
        assertEquals(2, read.get("b").getAsInt());
        assertFalse(read.has("a"), "the old content is replaced, not merged");
        assertFalse(Files.exists(file.resolveSibling("features.json.tmp")), "the temporary file is gone");
    }

    private void assertMovedAside(String content) throws IOException {
        Path file = dir.resolve("features.json");
        Files.writeString(file, content);
        JsonObject read = JsonFiles.readObject(file);
        assertEquals(0, read.size(), "defaults are used");
        assertFalse(Files.exists(file), "the broken file is no longer in place");
        List<String> siblings;
        try (Stream<Path> files = Files.list(dir)) {
            siblings = files.map(path -> path.getFileName().toString()).toList();
        }
        assertEquals(1, siblings.size(), "exactly one file remains: " + siblings);
        assertTrue(siblings.getFirst().startsWith("features.json.broken-"), "it is the broken copy: " + siblings);
        assertEquals(content, Files.readString(dir.resolve(siblings.getFirst())), "with its content intact");
    }
}
