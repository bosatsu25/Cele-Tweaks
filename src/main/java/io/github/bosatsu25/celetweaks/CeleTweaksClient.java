package io.github.bosatsu25.celetweaks;

import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.hotkey.HotkeyManager;
import io.github.bosatsu25.celetweaks.runtime.CeleCommands;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import io.github.bosatsu25.celetweaks.runtime.SafeStartup;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only entry point. Every startup stage is isolated so one failure cannot stop the others. */
public final class CeleTweaksClient implements ClientModInitializer {
    public static final String MOD_ID = "celetweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SafeStartup.run("config", () -> CeleConfig.get().load());
        SafeStartup.run("hotkeys", HotkeyManager::init);
        SafeStartup.run("features", () -> FeatureManager.get().init());
        SafeStartup.run("commands", CeleCommands::register);
        LOGGER.info("Cele-Tweaks initialized with {} isolated startup failure(s)", SafeStartup.failures().size());
    }
}
