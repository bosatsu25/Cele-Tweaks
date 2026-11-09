package io.github.bosatsu25.celetweaks.feature.guard;

import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature.Rule;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item Use Guard: the rule syntax ({@code item_or_#tag: block_or_#tag ...}) and the verdict as a
 * decision table over "hand empty", "some rule governs the item" and "a governing rule lists the
 * target". The game test proves the verdict reaches the world; this proves the verdict itself.
 */
class ItemUseGuardRulesTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    // Instance fields: they must not touch the registries before the bootstrap above has run.
    private final BlockState stone = Blocks.STONE.defaultBlockState();
    private final BlockState dirt = Blocks.DIRT.defaultBlockState();
    private final BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
    private final List<Rule> rules = Rule.compile(List.of("minecraft:cobblestone: minecraft:stone"));

    // --- rule syntax --------------------------------------------------------------------------

    @Test
    void aRuleSplitsAtTheFirstColonSpaceSoIdsKeepTheirNamespace() {
        Rule rule = Rule.parse("minecraft:bone_meal: minecraft:grass_block minecraft:dirt");
        assertNotNull(rule);
        assertTrue(rule.items().matches(new ItemStack(Items.BONE_MEAL)));
        assertFalse(rule.items().matches(new ItemStack(Items.STICK)));
        assertTrue(rule.targets().matches(grass));
        assertTrue(rule.targets().matches(dirt));
        assertFalse(rule.targets().matches(stone));
    }

    @ParameterizedTest(name = "\"{0}\" is not a rule")
    @NullAndEmptySource
    @ValueSource(strings = {"minecraft:cobblestone", "cobblestone:stone", "cobblestone :stone", "cobblestone"})
    void aLineWithoutColonSpaceIsNotARule(String line) {
        assertNull(Rule.parse(line));
    }

    @Test
    void anEmptyRightHandSideGovernsTheItemButAllowsNoBlock() {
        Rule rule = Rule.parse("cobblestone: ");
        assertNotNull(rule);
        assertTrue(rule.items().matches(new ItemStack(Items.COBBLESTONE)));
        assertTrue(rule.targets().isEmpty());
        assertTrue(ItemUseGuardFeature.blocks(List.of(rule), new ItemStack(Items.COBBLESTONE), stone));
    }

    @Test
    void compileKeepsTheParsableLinesInOrderAndDropsTheRest() {
        List<Rule> rules = Rule.compile(Arrays.asList("cobblestone: stone", "not a rule", null, "stick: dirt"));
        assertEquals(2, rules.size());
        assertTrue(rules.get(0).items().matches(new ItemStack(Items.COBBLESTONE)));
        assertTrue(rules.get(1).items().matches(new ItemStack(Items.STICK)));
        assertEquals(List.of(), Rule.compile(null));
    }

    // --- verdict: decision table ----------------------------------------------------------------
    //
    //  hand empty | item governed | a governing rule lists target || blocked
    //  T          | -             | -                             || F   (R1)
    //  F          | F             | -                             || F   (R2)
    //  F          | T             | T                             || F   (R3)
    //  F          | T             | F                             || T   (R4)

    @Test
    void r1AnEmptyHandIsNeverBlocked() {
        assertFalse(ItemUseGuardFeature.blocks(rules, ItemStack.EMPTY, dirt));
        assertFalse(ItemUseGuardFeature.blocks(rules, null, dirt));
    }

    @Test
    void r2AnItemWithoutARuleIsNeverBlocked() {
        assertFalse(ItemUseGuardFeature.blocks(rules, new ItemStack(Items.STICK), dirt));
        assertFalse(ItemUseGuardFeature.blocks(List.of(), new ItemStack(Items.COBBLESTONE), dirt));
    }

    @Test
    void r3AGovernedItemOnAListedBlockPasses() {
        assertFalse(ItemUseGuardFeature.blocks(rules, new ItemStack(Items.COBBLESTONE), stone));
    }

    @Test
    void r4AGovernedItemOnAnUnlistedBlockIsBlocked() {
        assertTrue(ItemUseGuardFeature.blocks(rules, new ItemStack(Items.COBBLESTONE), dirt));
    }

    @Test
    void anyOneGoverningRuleThatListsTheTargetIsEnough() {
        List<Rule> two = Rule.compile(List.of("cobblestone: stone", "cobblestone: dirt"));
        assertFalse(ItemUseGuardFeature.blocks(two, new ItemStack(Items.COBBLESTONE), dirt), "the second rule allows dirt");
        assertFalse(ItemUseGuardFeature.blocks(two, new ItemStack(Items.COBBLESTONE), stone), "the first rule allows stone");
        assertTrue(ItemUseGuardFeature.blocks(two, new ItemStack(Items.COBBLESTONE), grass), "neither lists grass");
    }
}
