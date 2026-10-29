package io.github.bosatsu25.celetweaks.feature.guard;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Protects the blocks the player is actually standing on while the player is moving across them.
 * Uses the player's real horizontal speed and bounding-box footprint, not key states.
 */
public final class FootingGuardFeature extends Feature {
    public FootingGuardFeature() {
        super(FeatureDefinition.FOOTING_GUARD);
    }

    @Override
    public void init() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || !isEnabled()) return InteractionResult.PASS;
            return isProtectedFooting(player, pos, config(Config.class)) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    static boolean isProtectedFooting(Player player, BlockPos pos, Config cfg) {
        if (!player.onGround()) return false;
        if (cfg.allowWhileSneaking && player.isShiftKeyDown()) return false;
        double min = cfg.minSpeedHundredths / 100.0;
        if (player.getDeltaMovement().horizontalDistanceSqr() < min * min) return false;
        AABB box = player.getBoundingBox();
        int footY = Mth.floor(box.minY - 1.0E-3);
        if (pos.getY() != footY) return false;
        return pos.getX() >= Mth.floor(box.minX) && pos.getX() <= Mth.floor(box.maxX - 1.0E-7)
                && pos.getZ() >= Mth.floor(box.minZ) && pos.getZ() <= Mth.floor(box.maxZ - 1.0E-7);
    }

    public static final class Config {
        /** Minimum horizontal speed, in hundredths of a block per tick, for the guard to apply. */
        @Range(min = 0, max = 100) public int minSpeedHundredths = 3;
        /** Sneaking is a deliberate "I mean it" signal, so it lifts the guard by default. */
        public boolean allowWhileSneaking = true;
    }
}
