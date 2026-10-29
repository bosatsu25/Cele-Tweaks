package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

/** Blocks the Syncmatica "remove shared placement" button, or requires Shift for it. */
public final class SharedPlanLockFeature extends Feature {
    public enum Mode { DISABLE_REMOVE, REQUIRE_SHIFT }

    private static boolean warned;

    public SharedPlanLockFeature() {
        super(FeatureDefinition.SHARED_PLAN_LOCK);
    }

    /** @param listener Syncmatica's button listener; its enum field tells which button was pressed. */
    public static boolean shouldBlock(Object listener) {
        if (!CeleConfig.get().isEnabled(FeatureDefinition.SHARED_PLAN_LOCK) || !isRemove(listener)) return false;
        Mode mode = CeleConfig.get().section(FeatureDefinition.SHARED_PLAN_LOCK, Config.class).mode;
        Minecraft client = Minecraft.getInstance();
        boolean block = mode != Mode.REQUIRE_SHIFT || !client.hasShiftDown();
        if (block && client.player != null) {
            client.player.sendOverlayMessage(Component.translatable(
                    mode == Mode.REQUIRE_SHIFT ? "celetweaks.shared_plan_lock.shift" : "celetweaks.shared_plan_lock.blocked"));
        }
        return block;
    }

    private static boolean isRemove(Object listener) {
        if (listener == null) return false;
        for (Class<?> type = listener.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isEnum()) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(listener);
                    if (value instanceof Enum<?> constant) return constant.name().contains("REMOVE");
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    break;
                }
            }
        }
        if (!warned) {
            warned = true;
            CeleTweaksClient.LOGGER.warn("Shared Plan Lock could not identify Syncmatica's button type; it will not block anything");
        }
        return false;
    }

    public static final class Config {
        public Mode mode = Mode.REQUIRE_SHIFT;
    }
}
