package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.config.Ids;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
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
    private record Conversion(IdMatcher world, String schematicId, IdMatcher tool) {}
    private record StandIn(String schematicId, String placeableId) {}

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
        String s = Ids.of(schematic);
        String p = Ids.of(placed);
        for (StandIn rule : standIns()) {
            if (rule.schematicId().equals(s) && rule.placeableId().equals(p)) return true;
        }
        return false;
    }

    public static boolean acceptsConversion(Player player, BlockState world, BlockState schematic) {
        String schematicId = Ids.of(schematic.getBlock());
        Config cfg = config();
        if (cfg.builtInAxeStripping && player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES)) {
            Identifier worldKey = BuiltInRegistries.BLOCK.getKey(world.getBlock());
            if (schematicId.equals(worldKey.getNamespace() + ":stripped_" + worldKey.getPath())) return true;
        }
        for (Conversion rule : conversions()) {
            if (rule.schematicId().equals(schematicId) && rule.world().matches(world) && rule.tool().matches(player.getMainHandItem())) {
                return true;
            }
        }
        return false;
    }

    private static Config config() {
        return CeleConfig.get().section(FeatureDefinition.PLACEMENT_CONVERSIONS, Config.class);
    }

    private static List<Conversion> conversions() {
        List<String> source = config().conversions;
        if (!source.equals(compiledConversionsFrom)) {
            List<Conversion> out = new ArrayList<>();
            for (String line : source) {
                int arrow = line.indexOf("->");
                int with = line.indexOf(" with ");
                if (arrow < 0 || with < arrow) continue;
                String target = Ids.normalize(line.substring(arrow + 2, with));
                if (target == null) continue;
                out.add(new Conversion(IdMatcher.parse(line.substring(0, arrow)), target, IdMatcher.parse(line.substring(with + 6))));
            }
            conversions = List.copyOf(out);
            compiledConversionsFrom = List.copyOf(source);
        }
        return conversions;
    }

    private static List<StandIn> standIns() {
        List<String> source = config().standIns;
        if (!source.equals(compiledStandInsFrom)) {
            List<StandIn> out = new ArrayList<>();
            for (String line : source) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String schematic = Ids.normalize(line.substring(0, eq));
                String placeable = Ids.normalize(line.substring(eq + 1));
                if (schematic != null && placeable != null) out.add(new StandIn(schematic, placeable));
            }
            standIns = List.copyOf(out);
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
