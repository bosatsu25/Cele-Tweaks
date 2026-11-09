package io.github.bosatsu25.celetweaks.feature.guard;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based guard for using an item on a block. A rule reads {@code item_or_#tag: block_or_#tag ...};
 * an item covered by a rule may only be used on the blocks listed on its right-hand side.
 * Items without a rule are never affected.
 */
public final class ItemUseGuardFeature extends Feature {
    /** One compiled rule line: the items it governs and the blocks they may be used on. */
    record Rule(IdMatcher items, IdMatcher targets) {
        /** The separator is the first ": " (colon followed by a space), so ids like minecraft:x stay intact. */
        static final String SEPARATOR = ": ";

        /** Parses one config line, or returns null when the line has no separator. */
        static Rule parse(String line) {
            if (line == null) return null;
            int split = line.indexOf(SEPARATOR);
            if (split < 0) return null;
            return new Rule(IdMatcher.parse(line.substring(0, split)), IdMatcher.parse(line.substring(split + SEPARATOR.length())));
        }

        /** Compiles every parsable line, in order; lines without a separator are dropped. */
        static List<Rule> compile(List<String> lines) {
            if (lines == null) return List.of();
            List<Rule> compiled = new ArrayList<>();
            for (String line : lines) {
                Rule rule = parse(line);
                if (rule != null) compiled.add(rule);
            }
            return List.copyOf(compiled);
        }
    }

    private List<String> compiledFrom = List.of();
    private List<Rule> rules = List.of();

    public ItemUseGuardFeature() {
        super(FeatureDefinition.ITEM_USE_GUARD);
    }

    @Override
    public void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !isEnabled()) return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (!blocks(rules(), stack, level.getBlockState(hit.getBlockPos()))) return InteractionResult.PASS;
            player.sendOverlayMessage(Component.translatable("celetweaks.item_use_guard.blocked"));
            return InteractionResult.FAIL;
        });
    }

    /**
     * The guard's verdict: true when {@code stack} is governed by at least one rule and no rule
     * governing it lists {@code target}. An empty hand and ungoverned items are never blocked.
     */
    static boolean blocks(List<Rule> rules, ItemStack stack, BlockState target) {
        if (stack == null || stack.isEmpty()) return false;
        boolean governed = false;
        for (Rule rule : rules) {
            if (!rule.items().matches(stack)) continue;
            governed = true;
            if (rule.targets().matches(target)) return false;
        }
        return governed;
    }

    private List<Rule> rules() {
        List<String> source = config(Config.class).rules;
        if (source == null) source = List.of();
        if (!source.equals(compiledFrom)) {
            rules = Rule.compile(source);
            compiledFrom = List.copyOf(source);
        }
        return rules;
    }

    public static final class Config {
        /** Example: "minecraft:bone_meal: #minecraft:saplings minecraft:grass_block". */
        public List<String> rules = new ArrayList<>();
    }
}
