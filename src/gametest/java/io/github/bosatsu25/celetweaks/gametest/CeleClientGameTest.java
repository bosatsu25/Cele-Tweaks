package io.github.bosatsu25.celetweaks.gametest;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.catalog.Tier;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.gui.FeatureListScreen;
import io.github.bosatsu25.celetweaks.gui.FeatureSettingsScreen;
import io.github.bosatsu25.celetweaks.runtime.FeatureManager;
import io.github.bosatsu25.celetweaks.runtime.SafeStartup;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * Launch check run by CI on every version. The real client starts with the mod, every settings
 * screen opens, and a singleplayer world is played with every vanilla feature switched on.
 * Required mixins crash the game when their target is wrong, so reaching the end means they all
 * applied; any startup failure or quarantined feature fails the test.
 */
public final class CeleClientGameTest implements FabricClientGameTest {
    private static final List<FeatureDefinition> VANILLA = FeatureDefinition.VALUES.stream()
            .filter(definition -> definition.tier() != Tier.INTEGRATION)
            .toList();

    /** Puts something on screen for each world-facing feature, next to the player. */
    private static final List<String> SCENE = List.of(
            // Scoreboard Panel + Quiet UI
            "scoreboard objectives add cele_test dummy",
            "scoreboard objectives setdisplay sidebar cele_test",
            "scoreboard players set @a cele_test 7",
            "team add cele_team",
            "team modify cele_team color red",
            "team join cele_team @a",
            // Quiet Chat
            "tellraw @a {\"text\":\"Cele-Tweaks game test\",\"color\":\"red\"}",
            // Lava Highlight + Range Overlay
            "execute at @p run setblock ~2 ~ ~ minecraft:lava",
            "execute at @p run setblock ~-2 ~ ~ minecraft:beacon",
            "execute at @p run setblock ~ ~ ~3 minecraft:lightning_rod");

    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> startupFailures = context.computeOnClient(client -> SafeStartup.failures());
        check(startupFailures.isEmpty(), "startup stages failed: " + startupFailures);

        Screen title = context.computeOnClient(client -> Compat.currentScreen());
        context.runOnClient(client -> VANILLA.forEach(definition -> CeleConfig.get().setEnabled(definition, true)));

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
