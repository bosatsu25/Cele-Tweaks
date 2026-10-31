package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.CeleTweaksClient;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/** Hotkey that asks Litematica to recompute its material list. Reflection only; no Litematica compile dependency. */
public final class MaterialListRecountFeature extends Feature {
    public static final String ACTION = "material_list.recount";

    public MaterialListRecountFeature() {
        super(FeatureDefinition.MATERIAL_LIST_RECOUNT);
    }

    @Override
    public void init() {
        HotkeyManager.register(ACTION, Component.translatable("celetweaks.hotkey.material_list_recount"), this::refresh);
    }

    private void refresh() {
        if (!isEnabled()) return;
        try {
            Class<?> dataManager = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Object list = dataManager.getMethod("getMaterialList").invoke(null);
            if (list == null) return;
            Method refresh = list.getClass().getMethod("refreshPreFilteredList");
            refresh.invoke(list);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.translatable("celetweaks.material_list.recounted"));
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            CeleTweaksClient.LOGGER.debug("Litematica material list recount unavailable: {}", failure.toString());
        }
    }
}
