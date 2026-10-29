package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.IdMatcher;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps TweakerMore's schematic auto-pick away from slots and items the player wants to keep:
 * hotbar slots locked by Hotbar Lock, and any held item listed here.
 */
public final class AutoPickScopeFeature extends Feature {
    public AutoPickScopeFeature() {
        super(FeatureDefinition.AUTO_PICK_SCOPE);
    }

    public static boolean allowPick(LocalPlayer player, InteractionHand hand) {
        if (player == null || !CeleConfig.get().isEnabled(FeatureDefinition.AUTO_PICK_SCOPE)) return true;
        Config cfg = CeleConfig.get().section(FeatureDefinition.AUTO_PICK_SCOPE, Config.class);
        if (cfg.respectHotbarLock && hand == InteractionHand.MAIN_HAND
                && HotbarLockFeature.isLocked(player.getInventory().getSelectedSlot())) {
            return false;
        }
        return !IdMatcher.of(cfg.keepItems).matches(player.getItemInHand(hand));
    }

    public static final class Config {
        public boolean respectHotbarLock = true;
        /** Item ids or #tags that auto-pick must not replace in hand. */
        public List<String> keepItems = new ArrayList<>();
    }
}
