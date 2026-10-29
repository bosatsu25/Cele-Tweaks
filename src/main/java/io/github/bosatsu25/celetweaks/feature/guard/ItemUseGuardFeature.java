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
    private record Rule(IdMatcher items, IdMatcher targets) {}

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
            if (stack.isEmpty()) return InteractionResult.PASS;
            BlockState target = level.getBlockState(hit.getBlockPos());
            boolean governed = false;
            for (Rule rule : rules()) {
                if (!rule.items().matches(stack)) continue;
                governed = true;
                if (rule.targets().matches(target)) return InteractionResult.PASS;
            }
            if (!governed) return InteractionResult.PASS;
            player.sendOverlayMessage(Component.translatable("celetweaks.item_use_guard.blocked"));
            return InteractionResult.FAIL;
        });
    }

    private List<Rule> rules() {
        List<String> source = config(Config.class).rules;
        if (source == null) source = List.of();
        if (!source.equals(compiledFrom)) {
            List<Rule> compiled = new ArrayList<>();
            for (String line : source) {
                // A rule separator is the first ": " (colon followed by whitespace), so ids like minecraft:x stay intact.
                int split = line.indexOf(": ");
                if (split < 0) continue;
                compiled.add(new Rule(IdMatcher.parse(line.substring(0, split)), IdMatcher.parse(line.substring(split + 2))));
            }
            rules = List.copyOf(compiled);
            compiledFrom = List.copyOf(source);
        }
        return rules;
    }

    public static final class Config {
        /** Example: "minecraft:bone_meal: #minecraft:saplings minecraft:grass_block". */
        public List<String> rules = new ArrayList<>();
    }
}
