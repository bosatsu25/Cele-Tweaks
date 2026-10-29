package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.lang.reflect.Method;

/**
 * Load-aware tuning of TweakerMore's server data syncer. The number of block entities in loaded chunks
 * around the player is sampled; above the "enter" threshold the syncer is relaxed, and only after it
 * falls below the lower "exit" threshold (hysteresis) are the player's own TweakerMore values restored.
 * Values are also restored when the feature is disabled, the world changes or the component is quarantined.
 */
public final class SyncerTuneFeature extends Feature {
    private Object intervalOption;
    private Object limitOption;
    private Method getter;
    private Method setter;
    private boolean resolved;
    private boolean relaxed;
    private int savedInterval;
    private int savedLimit;
    private int ticks;

    public SyncerTuneFeature() {
        super(FeatureDefinition.SYNCER_TUNE);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            restore();
            return;
        }
        Config cfg = config(Config.class);
        if (++ticks < Math.max(1, cfg.checkIntervalTicks)) return;
        ticks = 0;
        if (!resolve()) return;
        int load = countBlockEntities(client, Math.max(1, Math.min(16, cfg.chunkRadius)));
        int enter = Math.max(1, cfg.enterThreshold);
        int exit = Math.min(enter, Math.max(0, cfg.exitThreshold));
        if (!relaxed && load >= enter) relax(cfg);
        else if (relaxed && load < exit) restore();
    }

    @Override
    public void onToggle(FeatureDefinition definition, boolean enabled) {
        if (!enabled) restore();
    }

    @Override
    public void resetSession(Minecraft client) {
        restore();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        restore();
    }

    private static int countBlockEntities(Minecraft client, int radius) {
        int centerX = client.player.blockPosition().getX() >> 4;
        int centerZ = client.player.blockPosition().getZ() >> 4;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = client.level.getChunkSource().getChunk(centerX + dx, centerZ + dz, ChunkStatus.FULL, false);
                if (chunk != null) count += chunk.getBlockEntities().size();
            }
        }
        return count;
    }

    private void relax(Config cfg) {
        try {
            savedInterval = (int) getter.invoke(intervalOption);
            savedLimit = (int) getter.invoke(limitOption);
            setter.invoke(intervalOption, Math.max(1, Math.min(100, cfg.relaxedQueryInterval)));
            setter.invoke(limitOption, Math.max(1, Math.min(8192, cfg.relaxedQueryLimit)));
            relaxed = true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            CeleTweaksClient.LOGGER.warn("Syncer Tune could not change TweakerMore settings", failure);
        }
    }

    private void restore() {
        if (!relaxed) return;
        relaxed = false;
        try {
            setter.invoke(intervalOption, savedInterval);
            setter.invoke(limitOption, savedLimit);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            CeleTweaksClient.LOGGER.warn("Syncer Tune could not restore TweakerMore settings", failure);
        }
    }

    private boolean resolve() {
        if (resolved) return intervalOption != null;
        resolved = true;
        try {
            Class<?> configs = Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs");
            intervalOption = configs.getField("SERVER_DATA_SYNCER_QUERY_INTERVAL").get(null);
            limitOption = configs.getField("SERVER_DATA_SYNCER_QUERY_LIMIT").get(null);
            getter = intervalOption.getClass().getMethod("getIntegerValue");
            setter = intervalOption.getClass().getMethod("setIntegerValue", int.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            intervalOption = null;
            CeleTweaksClient.LOGGER.info("Syncer Tune: TweakerMore server data syncer settings unavailable ({})", failure.toString());
            return false;
        }
    }

    public static final class Config {
        @Range(min = 1, max = 16) public int chunkRadius = 5;
        @Range(min = 5, max = 1200) public int checkIntervalTicks = 40;
        /** Relax when at least this many block entities are loaded nearby. */
        @Range(min = 1, max = 100000) public int enterThreshold = 400;
        /** Restore once the count falls below this (must not exceed the enter threshold). */
        @Range(min = 0, max = 100000) public int exitThreshold = 250;
        @Range(min = 1, max = 100) public int relaxedQueryInterval = 3;
        @Range(min = 1, max = 8192) public int relaxedQueryLimit = 128;
    }
}
