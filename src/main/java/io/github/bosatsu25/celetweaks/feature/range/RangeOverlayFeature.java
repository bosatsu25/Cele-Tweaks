package io.github.bosatsu25.celetweaks.feature.range;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Outlines beacon effect ranges and lightning rod attraction radii for blocks in loaded chunks near the
 * player. Drawn through vanilla's per-tick gizmo collector, which exists with the same API on 26.1.2,
 * 26.2 and 26.3. Positions are rescanned on an interval; nothing is ever loaded or requested.
 */
public final class RangeOverlayFeature extends Feature {
    private record Beacon(BlockPos pos, int levels) {}

    private final List<Beacon> beacons = new ArrayList<>();
    private final List<BlockPos> rods = new ArrayList<>();
    private int ticksUntilScan;

    public RangeOverlayFeature() {
        super(FeatureDefinition.RANGE_OVERLAY);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            clear();
            return;
        }
        Config cfg = config(Config.class);
        if (--ticksUntilScan <= 0) {
            ticksUntilScan = Math.max(5, cfg.rescanIntervalTicks);
            scan(client.level, client.player.blockPosition(), cfg);
        }
        draw(client, cfg);
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    private void clear() {
        beacons.clear();
        rods.clear();
        ticksUntilScan = 0;
    }

    private void scan(ClientLevel level, BlockPos center, Config cfg) {
        beacons.clear();
        rods.clear();
        if (cfg.showBeacons) {
            int radius = Math.max(1, Math.min(16, cfg.beaconChunkRadius));
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunk(cx + dx, cz + dz, ChunkStatus.FULL, false);
                    if (chunk == null) continue;
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof BeaconBlockEntity) {
                            int levels = pyramidLevels(level, blockEntity.getBlockPos());
                            if (levels > 0) beacons.add(new Beacon(blockEntity.getBlockPos().immutable(), levels));
                        }
                    }
                }
            }
        }
        if (cfg.showLightningRods) {
            int h = Math.max(1, Math.min(64, cfg.rodScanRadius));
            int v = Math.max(1, Math.min(64, cfg.rodScanVertical));
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = center.getY() - v; y <= center.getY() + v; y++) {
                for (int x = center.getX() - h; x <= center.getX() + h; x++) {
                    for (int z = center.getZ() - h; z <= center.getZ() + h; z++) {
                        cursor.set(x, y, z);
                        if (!level.isLoaded(cursor)) continue;
                        if (level.getBlockState(cursor).getBlock() instanceof LightningRodBlock) rods.add(cursor.immutable());
                    }
                }
            }
        }
    }

    /** Same rule as vanilla: each layer i below the beacon must be a full (2i+1)^2 square of beacon base blocks. */
    private static int pyramidLevels(ClientLevel level, BlockPos beacon) {
        int levels = 0;
        for (int i = 1; i <= 4; i++) {
            int y = beacon.getY() - i;
            for (int x = beacon.getX() - i; x <= beacon.getX() + i; x++) {
                for (int z = beacon.getZ() - i; z <= beacon.getZ() + i; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.BEACON_BASE_BLOCKS)) return levels;
                }
            }
            levels = i;
        }
        return levels;
    }

    private void draw(Minecraft client, Config cfg) {
        if (beacons.isEmpty() && rods.isEmpty()) return;
        try (Gizmos.TemporaryCollection ignored = client.collectPerTickGizmos()) {
            for (Beacon beacon : beacons) {
                int range = beacon.levels() * 10 + 10;
                BlockPos p = beacon.pos();
                AABB box = new AABB(p.getX() - range, p.getY() - range, p.getZ() - range,
                        p.getX() + 1 + range, p.getY() + 1 + range + client.level.getHeight(), p.getZ() + 1 + range);
                Gizmos.cuboid(box, GizmoStyle.stroke(cfg.beaconColor));
            }
            for (BlockPos rod : rods) {
                Gizmos.circle(Vec3.atCenterOf(rod), (float) cfg.rodRadius, GizmoStyle.stroke(cfg.rodColor));
                Gizmos.cuboid(rod, GizmoStyle.stroke(cfg.rodColor));
            }
        }
    }

    public static final class Config {
        public boolean showBeacons = true;
        public boolean showLightningRods = true;
        @Range(min = 1, max = 16) public int beaconChunkRadius = 6;
        @Range(min = 1, max = 64) public int rodScanRadius = 32;
        @Range(min = 1, max = 64) public int rodScanVertical = 24;
        /** Lightning rods attract lightning within this many blocks (Java Edition: 128). */
        @Range(min = 1, max = 256) public int rodRadius = 128;
        @Range(min = 5, max = 1200) public int rescanIntervalTicks = 40;
        /** ARGB. */
        public int beaconColor = 0xC040A0FF;
        public int rodColor = 0xC0FFD040;
    }
}
