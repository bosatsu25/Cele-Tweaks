package io.github.bosatsu25.celetweaks.config;

import io.github.bosatsu25.celetweaks.testsupport.GameBootstrap;
import io.github.bosatsu25.celetweaks.testsupport.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IdMatcher}: how a configured id list is compiled and matched. Partitions of the input list
 * (nothing, blanks, ids, tags, junk) and of the thing matched (listed, unlisted, empty stack).
 * Tag membership needs the game's data: without it the registry throws "Tags not bound" on any tag
 * query, so only the parsing of tags is checked here and matching through a tag is proven by the
 * client game test.
 */
class IdMatcherTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    @Test
    void nothingToMatchCompilesToTheSharedEmptyMatcher() {
        assertSame(IdMatcher.EMPTY, IdMatcher.of(null));
        assertSame(IdMatcher.EMPTY, IdMatcher.of(List.of()));
        assertSame(IdMatcher.EMPTY, IdMatcher.parse(null));
        assertSame(IdMatcher.EMPTY, IdMatcher.parse("   "));
        assertTrue(IdMatcher.EMPTY.isEmpty());
    }

    @Test
    void junkEntriesAreDroppedAndTheRestKept() {
        IdMatcher matcher = IdMatcher.of(Arrays.asList(null, "", "  ", "Bad Id", "#bad id", "stone"));
        assertFalse(matcher.isEmpty());
        assertTrue(matcher.matches(Blocks.STONE.defaultBlockState()));
        assertFalse(matcher.matches(Blocks.COBBLESTONE.defaultBlockState()));
    }

    @Test
    void onlyJunkCompilesToAnEmptyMatcherThatMatchesNothing() {
        IdMatcher matcher = IdMatcher.of(List.of("Bad Id", "#bad id", " "));
        assertTrue(matcher.isEmpty());
        assertFalse(matcher.matches(Blocks.STONE.defaultBlockState()));
        assertFalse(matcher.matches(new ItemStack(Items.STONE)));
        assertFalse(matcher.matches(Registry.entity("minecraft:pig")));
    }

    @Test
    void parseSplitsOnAnyWhitespace() {
        IdMatcher matcher = IdMatcher.parse(" stone \t minecraft:dirt\ncobblestone ");
        assertTrue(matcher.matches(Blocks.STONE.defaultBlockState()));
        assertTrue(matcher.matches(Blocks.DIRT.defaultBlockState()));
        assertTrue(matcher.matches(Blocks.COBBLESTONE.defaultBlockState()));
        assertFalse(matcher.matches(Blocks.SAND.defaultBlockState()));
    }

    @Test
    void anIdMatchesTheSameRegistryEntryAsBlockItemOrEntity() {
        IdMatcher matcher = IdMatcher.of(List.of("minecraft:stone", "pig"));
        assertTrue(matcher.matches(Blocks.STONE.defaultBlockState()));
        assertTrue(matcher.matches(new ItemStack(Items.STONE)));
        assertFalse(matcher.matches(new ItemStack(Items.COBBLESTONE)));
        assertTrue(matcher.matches(Registry.entity("minecraft:pig")));
        assertFalse(matcher.matches(Registry.entity("minecraft:cow")));
    }

    @Test
    void anEmptyOrMissingStackNeverMatches() {
        IdMatcher matcher = IdMatcher.of(List.of("minecraft:air", "minecraft:stone"));
        assertFalse(matcher.matches(ItemStack.EMPTY));
        assertFalse(matcher.matches((ItemStack) null));
    }

    @Test
    void aTagEntryCompilesToATagMatcher() {
        assertFalse(IdMatcher.of(List.of("#minecraft:logs")).isEmpty(), "a tag counts as content even though no id was listed");
        assertFalse(IdMatcher.parse("#minecraft:logs #c:ores").isEmpty());
    }

    @Test
    void idsAreCheckedBeforeTagsSoAListedIdMatchesEvenWhenTagsAreUnbound() {
        IdMatcher matcher = IdMatcher.of(List.of("#minecraft:logs", "stone"));
        assertTrue(matcher.matches(Blocks.STONE.defaultBlockState()));
        assertTrue(matcher.matches(new ItemStack(Items.STONE)));
    }
}
