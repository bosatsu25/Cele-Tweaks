package io.github.bosatsu25.celetweaks.feature.lava;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Makes lava sources stand out: source blocks get a tint colour and flowing lava is dimmed, applied to
 * vertex colours in the vanilla fluid mesher (FluidRenderer). Sodium replaces that mesher, so the
 * effect does not show while Sodium is installed.
 */
public final class LavaSourceTintFeature extends Feature {
    /** Fluid being meshed on this (chunk-builder) thread; set by the FluidRenderer mixin. */
    private static final ThreadLocal<FluidState> CURRENT = new ThreadLocal<>();

    private static volatile boolean active;
    private static volatile int sourceTint;
    private static volatile int flowingBrightness;

    public LavaSourceTintFeature() {
        super(FeatureDefinition.LAVA_SOURCE_TINT);
    }

    @Override
    public void init() {
        refresh();
    }

    @Override
    public void onToggle(FeatureDefinition definition, boolean enabled) {
        refresh();
        Compat.reloadChunks(Minecraft.getInstance());
    }

    @Override
    public void onQuarantined(Minecraft client) {
        active = false;
        Compat.reloadChunks(client);
    }

    private void refresh() {
        Config cfg = config(Config.class);
        sourceTint = cfg.sourceTint & 0xFFFFFF;
        int b = Math.max(0, Math.min(100, cfg.flowingBrightnessPercent)) * 255 / 100;
        flowingBrightness = (b << 16) | (b << 8) | b;
        active = CeleConfig.get().isEnabled(definition);
    }

    public static void beginFluid(FluidState state) {
        if (active) CURRENT.set(state);
    }

    public static void endFluid() {
        CURRENT.remove();
    }

    /** Multiplies the vertex colour by the configured tint while lava is being meshed. */
    public static int tintVertex(int argb) {
        if (!active) return argb;
        FluidState state = CURRENT.get();
        if (state == null || !state.is(Fluids.LAVA) && !state.is(Fluids.FLOWING_LAVA)) return argb;
        int tint = state.isSource() ? sourceTint : flowingBrightness;
        int r = ((argb >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
        int g = ((argb >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
        int b = (argb & 0xFF) * (tint & 0xFF) / 255;
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static final class Config {
        /** RGB multiplied with source blocks (0xFFFFFF = unchanged). */
        public int sourceTint = 0xFFF0A0;
        /** Brightness of flowing lava, percent (100 = unchanged). */
        @io.github.bosatsu25.celetweaks.config.Range(min = 0, max = 100) public int flowingBrightnessPercent = 40;
    }
}
