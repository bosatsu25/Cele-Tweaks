package io.github.bosatsu25.celetweaks.gametest;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.gui.FeatureListScreen;
import io.github.bosatsu25.celetweaks.gui.FeatureSettingsScreen;
import io.github.bosatsu25.celetweaks.mixin.integration.IntegrationMixinPlugin;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import io.github.bosatsu25.celetweaks.runtime.SafeStartup;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Launch check run by CI on every version. The real client starts with the mod, every settings
 * screen opens, and a singleplayer world is played with every available feature switched on.
 * Required mixins crash the game when their target is wrong, so reaching the end means they all
 * applied; any startup failure or quarantined feature fails the test. When CI's integration job adds
 * the external mods, every integration mixin must also have hooked into its target.
 */
public final class CeleClientGameTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("celetweaks-gametest");

    /** Puts something on screen for each world-facing feature, next to the player. */
    private static final List<String> SCENE = List.of(
            "gamemode creative @a",
            // Sidebar Layout + Grey Team Colors
            "scoreboard objectives add cele_test dummy",
            "scoreboard objectives setdisplay sidebar cele_test",
            "scoreboard players set @a cele_test 7",
            "team add cele_team",
            "team modify cele_team color red",
            "team join cele_team @a",
            // Grey Chat Colors
            "tellraw @a {\"text\":\"Cele-Tweaks game test\",\"color\":\"red\"}",
            // Lava Source Tint (sunk into the ground so it cannot flow) + Range Overlay
            "execute at @p run setblock ~3 ~-1 ~ minecraft:lava",
            "execute at @p run setblock ~-2 ~ ~ minecraft:beacon",
            "execute at @p run setblock ~ ~ ~3 minecraft:lightning_rod");

    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> startupFailures = context.computeOnClient(client -> SafeStartup.failures());
        check(startupFailures.isEmpty(), "startup stages failed: " + startupFailures);

        Screen title = context.computeOnClient(client -> Compat.currentScreen());
        List<FeatureDefinition> available = FeatureDefinition.VALUES.stream().filter(FeatureDefinition::isAvailable).toList();
        LOGGER.info("Switching on {} available feature(s): {}", available.size(), available);
        context.runOnClient(client -> available.forEach(definition -> CeleConfig.get().setEnabled(definition, true)));

        context.setScreen(() -> new FeatureListScreen(title));
        context.waitTicks(2);
        context.takeScreenshot("celetweaks-settings");
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            context.setScreen(() -> new FeatureSettingsScreen(title, definition));
            context.waitTick();
        }
        context.setScreen(() -> title);
        checkNothingQuarantined(context, "settings screens");

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            SCENE.forEach(world.getServer()::runCommand);
            context.waitTicks(100);

            // Re-mesh path of Block Filter (version-specific Compat.reloadChunks) while a level is loaded.
            context.runOnClient(client -> {
                CeleConfig.get().setEnabled(FeatureDefinition.BLOCK_FILTER, false);
                CeleConfig.get().setEnabled(FeatureDefinition.BLOCK_FILTER, true);
            });
            context.waitTicks(20);

            context.takeScreenshot("celetweaks-world");
            checkNothingQuarantined(context, "singleplayer world");
        }
        checkIntegrationHooks();
    }

    /** With external mods installed, each selected integration mixin must have woven at least one call. */
    private static void checkIntegrationHooks() {
        Map<String, String> selected = IntegrationMixinPlugin.selected();
        if (selected.isEmpty()) return;
        for (String target : selected.values()) {
            try {
                // Loading the class (without initialising it) is what weaves the mixin into it.
                Class.forName(target, false, CeleClientGameTest.class.getClassLoader());
            } catch (ClassNotFoundException missing) {
                throw new AssertionError("integration target class is gone: " + target, missing);
            }
        }
        Map<String, Integer> hooks = IntegrationMixinPlugin.hooks();
        LOGGER.info("Integration hooks (mixin -> call sites): {}", hooks);
        List<String> idle = selected.keySet().stream().filter(mixin -> hooks.getOrDefault(mixin, 0) == 0).sorted().toList();
        check(idle.isEmpty(), "integration mixins that hooked nothing: " + idle + " (targets " + selected + ")");
    }

    private static void checkNothingQuarantined(ClientGameTestContext context, String stage) {
        List<FeatureDefinition> quarantined = context.computeOnClient(client -> FeatureDefinition.VALUES.stream()
                .filter(FeatureManager.get()::isQuarantined)
                .toList());
        check(quarantined.isEmpty(), stage + ": quarantined " + quarantined);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
