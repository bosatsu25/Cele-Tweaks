package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.config.Ids;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Teaches TweakerMore's schematic placement restriction two things, both written as rules:
 * <ul>
 *   <li>Conversions — "{@code world -> schematic with tool}": using a matching tool on a matching world
 *       block is accepted when the schematic expects the result (e.g. stripping a log with an axe).</li>
 *   <li>Stand-ins — "{@code schematic = placeable}": placing the right-hand block is accepted where the
 *       schematic has the left-hand one.</li>
 * </ul>
 * Built-in conversions cover vanilla axe stripping ("x" to "stripped_x"). It only relaxes TweakerMore's
 * verdict; it never places or interacts by itself.
 */
public final class PlacementConversionsFeature extends Feature {
    /** "{@code world -> schematic with tool}": tool on a world block gives the schematic block. */
    record Conversion(IdMatcher world, String schematicId, IdMatcher tool) {
        static final String ARROW = "->";
        static final String WITH = " with ";

        /** Parses one line, or returns null when it lacks the arrow, the " with " after it, or a valid target id. */
        static Conversion parse(String line) {
            if (line == null) return null;
            int arrow = line.indexOf(ARROW);
            int with = line.indexOf(WITH);
            if (arrow < 0 || with < arrow) return null;
            String target = Ids.normalize(line.substring(arrow + ARROW.length(), with));
            if (target == null) return null;
            return new Conversion(IdMatcher.parse(line.substring(0, arrow)), target, IdMatcher.parse(line.substring(with + WITH.length())));
        }

        static List<Conversion> compile(List<String> lines) {
            if (lines == null) return List.of();
            List<Conversion> out = new ArrayList<>();
            for (String line : lines) {
                Conversion rule = parse(line);
                if (rule != null) out.add(rule);
            }
            return List.copyOf(out);
        }
    }

    /** "{@code schematic = placeable}": the right-hand block may stand where the schematic has the left-hand one. */
    record StandIn(String schematicId, String placeableId) {
        /** Parses one line, or returns null when it lacks "=" or either side is not a valid id. */
        static StandIn parse(String line) {
            if (line == null) return null;
            int eq = line.indexOf('=');
            if (eq < 0) return null;
            String schematic = Ids.normalize(line.substring(0, eq));
            String placeable = Ids.normalize(line.substring(eq + 1));
            if (schematic == null || placeable == null) return null;
            return new StandIn(schematic, placeable);
        }

        static List<StandIn> compile(List<String> lines) {
            if (lines == null) return List.of();
            List<StandIn> out = new ArrayList<>();
            for (String line : lines) {
                StandIn rule = parse(line);
                if (rule != null) out.add(rule);
            }
            return List.copyOf(out);
        }
    }

    private static volatile List<String> compiledConversionsFrom;
    private static volatile List<Conversion> conversions = List.of();
    private static volatile List<String> compiledStandInsFrom;
    private static volatile List<StandIn> standIns = List.of();

    public PlacementConversionsFeature() {
        super(FeatureDefinition.PLACEMENT_CONVERSIONS);
    }

    public static boolean active() {
        return CeleConfig.get().isEnabled(FeatureDefinition.PLACEMENT_CONVERSIONS);
    }

    public static boolean acceptsStandIn(Block schematic, Block placed) {
        return standsIn(standIns(), schematic, placed);
    }

    public static boolean acceptsConversion(Player player, BlockState world, BlockState schematic) {
        return converts(conversions(), config().builtInAxeStripping, player.getMainHandItem(), world, schematic);
    }

    /** True when a rule names exactly this schematic block and this placed block, in that order. */
    static boolean standsIn(List<StandIn> rules, Block schematic, Block placed) {
        String s = Ids.of(schematic);
        String p = Ids.of(placed);
        for (StandIn rule : rules) {
            if (rule.schematicId().equals(s) && rule.placeableId().equals(p)) return true;
        }
        return false;
    }

    /**
     * True when the held item applied to the world block is expected to give the schematic block:
     * either the built-in axe stripping ("x" to "stripped_x" with any axe, when switched on) or a rule
     * whose world and tool matchers both match and whose target is the schematic block.
     */
    static boolean converts(List<Conversion> rules, boolean builtInAxeStripping, ItemStack held, BlockState world, BlockState schematic) {
        String schematicId = Ids.of(schematic.getBlock());
        if (builtInAxeStripping && held.is(ItemTags.AXES)) {
            Identifier worldKey = BuiltInRegistries.BLOCK.getKey(world.getBlock());
            if (schematicId.equals(worldKey.getNamespace() + ":stripped_" + worldKey.getPath())) return true;
        }
        for (Conversion rule : rules) {
            if (rule.schematicId().equals(schematicId) && rule.world().matches(world) && rule.tool().matches(held)) return true;
        }
        return false;
    }

    private static Config config() {
        return CeleConfig.get().section(FeatureDefinition.PLACEMENT_CONVERSIONS, Config.class);
    }

    private static List<Conversion> conversions() {
        List<String> source = config().conversions;
        if (!source.equals(compiledConversionsFrom)) {
            conversions = Conversion.compile(source);
            compiledConversionsFrom = List.copyOf(source);
        }
        return conversions;
    }

    private static List<StandIn> standIns() {
        List<String> source = config().standIns;
        if (!source.equals(compiledStandInsFrom)) {
            standIns = StandIn.compile(source);
            compiledStandInsFrom = List.copyOf(source);
        }
        return standIns;
    }

    public static final class Config {
        public boolean builtInAxeStripping = true;
        /** "world_block_or_#tag -> schematic_block with item_or_#tag", e.g. "#minecraft:dirt -> minecraft:farmland with #minecraft:hoes". */
        public List<String> conversions = new ArrayList<>();
        /** "schematic_block = placeable_block". */
        public List<String> standIns = new ArrayList<>();
    }
}
