package io.github.bosatsu25.celetweaks.config;

import io.github.bosatsu25.celetweaks.testsupport.GameBootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link Ids#normalize}: equivalence partitions of the text a person may type into an id list.
 * Valid: a full id, a bare path (gets the minecraft namespace), either with surrounding spaces.
 * Invalid: nothing, blanks, upper case, spaces inside, a tag marker, a second colon.
 */
class IdsTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "minecraft:stone,          minecraft:stone",
            "stone,                    minecraft:stone",
            "'  stone  ',              minecraft:stone",
            "tweakermore:some_thing,   tweakermore:some_thing",
            "'minecraft:stone ',       minecraft:stone",
    })
    void validIdsAreNormalisedToNamespaceAndPath(String raw, String expected) {
        assertEquals(expected, Ids.normalize(raw));
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Stone", "minecraft:Stone", "mine craft:stone", "#minecraft:logs", "a:b:c", "minecraft:stone block"})
    void invalidTextGivesNull(String raw) {
        assertNull(Ids.normalize(raw));
    }

    @Test
    void normalizedSetKeepsOnlyTheValidEntriesOnce() {
        List<String> raw = Arrays.asList("stone", "minecraft:stone", "Bad", null, "", "dirt");
        assertEquals(Set.of("minecraft:stone", "minecraft:dirt"), Ids.normalizedSet(raw));
    }

    @Test
    void registryIdsReadBackAsFullIds() {
        assertEquals("minecraft:stone", Ids.of(Blocks.STONE));
        assertEquals("minecraft:stick", Ids.of(Items.STICK));
        assertEquals("minecraft:pig", Ids.of(EntityType.PIG));
    }
}
