package io.github.bosatsu25.celetweaks.feature.guard;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.RuntimeComponent;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Set;

/**
 * Portal-aware guards that use the portal's own orientation.
 * <ul>
 *   <li>Frame guard: an obsidian block is protected only when a neighbouring portal block lies in the
 *       same plane as the portal (i.e. the obsidian is part of that portal's frame).</li>
 *   <li>Face guard: a block may not be placed directly in front of or behind a portal opening, the two
 *       cells an entity walks through.</li>
 * </ul>
 */
public final class PortalProtectionFeature implements RuntimeComponent {
    @Override
    public String id() { return "portal_guard"; }

    @Override
    public Set<FeatureDefinition> definitions() {
        return Set.of(FeatureDefinition.PORTAL_FRAME_PROTECT, FeatureDefinition.PORTAL_DOORWAY_CLEAR);
    }

    @Override
    public void init() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || !CeleConfig.get().isEnabled(FeatureDefinition.PORTAL_FRAME_PROTECT)) return InteractionResult.PASS;
            if (isFrameOfActivePortal(level, pos)) {
                warn(player, "celetweaks.portal.frame");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide() || !CeleConfig.get().isEnabled(FeatureDefinition.PORTAL_DOORWAY_CLEAR)) return InteractionResult.PASS;
            if (!(player.getItemInHand(hand).getItem() instanceof BlockItem)) return InteractionResult.PASS;
            BlockPos clicked = hit.getBlockPos();
            BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
            if (blocksPortalFace(level, target)) {
                warn(player, "celetweaks.portal.face");
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    /** Obsidian whose neighbour is a portal block lying in the same plane as that portal. */
    static boolean isFrameOfActivePortal(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.OBSIDIAN)) return false;
        for (Direction side : Direction.values()) {
            BlockState neighbour = level.getBlockState(pos.relative(side));
            if (!neighbour.is(Blocks.NETHER_PORTAL)) continue;
            Direction.Axis portalAxis = neighbour.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            Direction.Axis facing = perpendicular(portalAxis);
            if (side.getAxis() != facing) return true;
        }
        return false;
    }

    /** True when {@code pos} is a portal cell or sits in front of / behind a portal opening. */
    static boolean blocksPortalFace(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) return true;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = level.getBlockState(pos.relative(side));
            if (!neighbour.is(Blocks.NETHER_PORTAL)) continue;
            if (side.getAxis() == perpendicular(neighbour.getValue(BlockStateProperties.HORIZONTAL_AXIS))) return true;
        }
        return false;
    }

    private static Direction.Axis perpendicular(Direction.Axis portalAxis) {
        return portalAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static void warn(Player player, String key) {
        player.sendOverlayMessage(Component.translatable(key));
    }
}
