package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature.Conversion;
import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature.StandIn;
import io.github.bosatsu25.celetweaks.testsupport.GameBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Placement Conversions: the two rule syntaxes and the two verdicts they feed. Stand-ins
 * ({@code schematic = placeable}) are an exact, ordered pair; conversions
 * ({@code world -> schematic with tool}) match the world block and the tool through id lists.
 * The built-in axe stripping goes through the axes tag, which needs game data, so it is proven by
 * the integration game test rather than here.
 */
class PlacementConversionRulesTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    // Instance fields: they must not touch the registries before the bootstrap above has run.
    private final BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
    private final BlockState strippedOakLog = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    private final BlockState birchLog = Blocks.BIRCH_LOG.defaultBlockState();
    private final BlockState strippedBirchLog = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
    private final BlockState spruceLog = Blocks.SPRUCE_LOG.defaultBlockState();

    // --- stand-ins -------------------------------------------------------------------------------

    @Test
    void aStandInNormalisesBothSidesAndIgnoresSpaces() {
        assertEquals(new StandIn("minecraft:stone", "minecraft:cobblestone"), StandIn.parse(" stone = minecraft:cobblestone "));
        assertEquals(new StandIn("minecraft:stone", "minecraft:cobblestone"), StandIn.parse("minecraft:stone=cobblestone"));
    }

    @ParameterizedTest(name = "\"{0}\" is not a stand-in")
    @NullAndEmptySource
    @ValueSource(strings = {"stone cobblestone", "Bad Id = stone", "stone = ", " = stone", "stone -> cobblestone"})
    void aStandInNeedsAnEqualsSignAndTwoValidIds(String line) {
        assertNull(StandIn.parse(line));
    }

    @Test
    void aStandInIsAnOrderedExactPair() {
        List<StandIn> rules = StandIn.compile(List.of("minecraft:stone = minecraft:cobblestone", "junk"));
        assertEquals(1, rules.size());
        assertTrue(PlacementConversionsFeature.standsIn(rules, Blocks.STONE, Blocks.COBBLESTONE));
        assertFalse(PlacementConversionsFeature.standsIn(rules, Blocks.COBBLESTONE, Blocks.STONE), "the rule is not symmetric");
        assertFalse(PlacementConversionsFeature.standsIn(rules, Blocks.DIRT, Blocks.COBBLESTONE));
        assertFalse(PlacementConversionsFeature.standsIn(List.of(), Blocks.STONE, Blocks.COBBLESTONE));
    }

    // --- conversions -----------------------------------------------------------------------------

    @Test
    void aConversionReadsWorldArrowTargetWithTool() {
        Conversion rule = Conversion.parse(" oak_log birch_log -> stripped_oak_log with iron_axe minecraft:stone_axe ");
        assertNotNull(rule);
        assertEquals("minecraft:stripped_oak_log", rule.schematicId());
        assertTrue(rule.world().matches(oakLog), "the world side is an id list");
        assertTrue(rule.world().matches(birchLog));
        assertFalse(rule.world().matches(spruceLog));
        assertTrue(rule.tool().matches(new ItemStack(Items.IRON_AXE)), "the tool side is an id list");
        assertTrue(rule.tool().matches(new ItemStack(Items.STONE_AXE)));
        assertFalse(rule.tool().matches(new ItemStack(Items.STICK)));
    }

    @ParameterizedTest(name = "\"{0}\" is not a conversion")
    @NullAndEmptySource
    @ValueSource(strings = {
            "oak_log stripped_oak_log with iron_axe",      // no arrow
            "oak_log -> stripped_oak_log",                 // no " with "
            "oak_log with iron_axe -> stripped_oak_log",   // " with " before the arrow
            "oak_log -> Stripped Log with iron_axe",       // target is not an id
            "oak_log ->  with iron_axe",                   // empty target
    })
    void aConversionNeedsArrowThenWithThenAValidTarget(String line) {
        assertNull(Conversion.parse(line));
    }

    @Test
    void aConversionAcceptsOnlyItsOwnWorldBlockTargetAndTool() {
        List<Conversion> rules = Conversion.compile(List.of("oak_log -> stripped_oak_log with iron_axe", "junk"));
        assertEquals(1, rules.size());
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        assertTrue(PlacementConversionsFeature.converts(rules, false, axe, oakLog, strippedOakLog));
        assertFalse(PlacementConversionsFeature.converts(rules, false, new ItemStack(Items.STICK), oakLog, strippedOakLog), "wrong tool");
        assertFalse(PlacementConversionsFeature.converts(rules, false, axe, birchLog, strippedOakLog), "wrong world block");
        assertFalse(PlacementConversionsFeature.converts(rules, false, axe, oakLog, strippedBirchLog), "wrong schematic block");
        assertFalse(PlacementConversionsFeature.converts(rules, false, ItemStack.EMPTY, oakLog, strippedOakLog), "empty hand");
    }

    @Test
    void withoutRulesAndWithoutBuiltInStrippingNothingConverts() {
        assertFalse(PlacementConversionsFeature.converts(List.of(), false, new ItemStack(Items.IRON_AXE), oakLog, strippedOakLog));
    }
}
