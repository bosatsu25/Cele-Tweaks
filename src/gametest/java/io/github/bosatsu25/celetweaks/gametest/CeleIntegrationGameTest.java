package io.github.bosatsu25.celetweaks.gametest;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition.Mods;
import io.github.bosatsu25.celetweaks.compat.Compat;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.feature.integration.PlacementConversionsFeature;
import io.github.bosatsu25.celetweaks.feature.integration.SyncerTuneFeature;
import io.github.bosatsu25.celetweaks.feature.integration.ToolSwitchScopeFeature;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Behaviour of the integration features against the real mods, run only by CI's integration job.
 * Each case calls the other mod's own code (by reflection, as the features do) and checks what
 * Cele-Tweaks changed about the answer, next to a control showing the mod's answer without it.
 * Where a feature works by cancelling the other mod's method, {@link Spies} counts the calls that ran
 * to completion. A mod that is not installed skips its cases.
 */
public final class CeleIntegrationGameTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("celetweaks-gametest");
    private static final int SETTLE = 10;
    /** Ticks {@link #eventually} waits for a change to show on the client before the case fails. */
    private static final int TIMEOUT = 100;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (Stream.of(Mods.LITEMATICA, Mods.TWEAKEROO, Mods.TWEAKERMORE, Mods.ITEMSCROLLER).noneMatch(CeleIntegrationGameTest::loaded)) {
            LOGGER.info("No integration mod installed; integration behaviour tests skipped");
            return;
        }
        context.runOnClient(client -> {
            for (FeatureDefinition definition : FeatureDefinition.VALUES) {
                if (definition.isAvailable()) CeleConfig.get().setEnabled(definition, true);
            }
        });

        List<String> ran = new ArrayList<>();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            TestServerContext server = world.getServer();
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition());
            server.runCommand("gamemode creative @a");
            context.waitTicks(20);

            if (loaded(Mods.TWEAKERMORE) && loaded(Mods.LITEMATICA)) { placementConversions(context); ran.add("Placement Conversions"); }
            if (loaded(Mods.LITEMATICA)) { materialSubstitution(context); ran.add("Material Substitution"); }
            if (loaded(Mods.LITEMATICA)) { schematicDragDrop(context); ran.add("Schematic Drag & Drop"); }
            if (loaded(Mods.TWEAKERMORE)) { syncerTune(context, server, p); ran.add("Syncer Tune"); }
            if (loaded(Mods.TWEAKERMORE)) { autoPickScope(context, p); ran.add("Auto Pick Scope"); }
            if (loaded(Mods.TWEAKEROO)) { toolSwitchScope(context, server, p); ran.add("Tool Switch Scope"); }
            if (loaded(Mods.ITEMSCROLLER)) { shiftToMassCraft(context, server); ran.add("Shift to Mass Craft"); }
        }
        LOGGER.info("Integration behaviour tests passed: {}", ran);
    }

    /** TweakerMore's placement restriction accepts a stand-in and an axe stripping only with the rules. */
    private static void placementConversions(ClientGameTestContext context) {
        String pkg = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.restrict.";
        Method blockCorrect = context.computeOnClient(client -> method(pkg + "PlacementRestrictor", "isBlockToPlaceCorrect", BlockState.class, BlockState.class));
        Method itemLoose = context.computeOnClient(client ->
                method(pkg + "LooseCaseChecker", "isLooseCheckSpecialCase", BlockState.class, ItemStack.class, ItemStack.class));
        Method interact = context.computeOnClient(client ->
                method(pkg + "BlockInteractionRestrictor", "checkInteract", Player.class, BlockState.class, BlockState.class));
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();

        setStandIns(context, List.of());
        check(!(boolean) call(context, blockCorrect, stone, cobblestone), "control: TweakerMore already accepts cobblestone for stone");
        check(!(boolean) call(context, itemLoose, stone, new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE)),
                "control: TweakerMore already accepts a cobblestone item for stone");

        setStandIns(context, List.of("minecraft:stone = minecraft:cobblestone"));
        check((boolean) call(context, blockCorrect, stone, cobblestone), "Placement Conversions: the stand-in rule did not accept cobblestone for stone");
        check(!(boolean) call(context, blockCorrect, stone, Blocks.DIRT.defaultBlockState()), "Placement Conversions: dirt was accepted for stone without a rule");
        check((boolean) call(context, itemLoose, stone, new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE)),
                "Placement Conversions: the stand-in rule did not accept a cobblestone item for stone");

        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState stripped = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        check("GOOD_INTERACTION".equals(interactVerdict(context, interact, Items.IRON_AXE, log, stripped)),
                "Placement Conversions: stripping an oak log with an axe was not accepted where the schematic has a stripped log");
        check("NO_INTERACTION".equals(interactVerdict(context, interact, Items.STICK, log, stripped)),
                "control: TweakerMore's verdict for a stick on an oak log changed");
    }

    /** Litematica asks for a dead coral block; with only the live one carried, that is what it gets. */
    private static void materialSubstitution(ClientGameTestContext context) {
        Method required = context.computeOnClient(client -> method("fi.dy.masa.litematica.materials.MaterialCache", "getRequiredBuildItemForState", BlockState.class));
        Object cache = context.computeOnClient(client ->
                reflect(() -> Class.forName("fi.dy.masa.litematica.materials.MaterialCache").getMethod("getInstance").invoke(null)));
        BlockState dead = Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState();

        Item withNothing = context.computeOnClient(client -> {
            client.player.getInventory().clearContent();
            return ((ItemStack) invoke(required, cache, dead)).getItem();
        });
        check(withNothing == Items.DEAD_TUBE_CORAL_BLOCK, "control: Litematica's required item for a dead tube coral block is " + withNothing);

        Item withLive = context.computeOnClient(client -> {
            client.player.getInventory().setItem(0, new ItemStack(Items.TUBE_CORAL_BLOCK));
            return ((ItemStack) invoke(required, cache, dead)).getItem();
        });
        check(withLive == Items.TUBE_CORAL_BLOCK, "Material Substitution: Litematica still asked for " + withLive + " while a live tube coral block was carried");
        context.runOnClient(client -> client.player.getInventory().clearContent());
    }

    /** A dropped .litematic lands in the schematics folder; a dropped .txt is refused. */
    private static void schematicDragDrop(ClientGameTestContext context) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("celetweaks-drop-source");
        Path schematic = dir.resolve("cele-drop-test.litematic");
        Path text = dir.resolve("cele-drop-test.txt");
        reflect(() -> {
            Files.createDirectories(dir);
            Files.writeString(schematic, "not a real schematic, only copied");
            Files.writeString(text, "must be refused");
            return null;
        });
        // Litematica resolves the folder through Minecraft.getInstance(), which only the client thread may call.
        Path base = context.computeOnClient(client ->
                reflect(() -> (Path) Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getSchematicsBaseDirectory").invoke(null)));

        // The save browser: Litematica 26.3 imports drops itself on its load and manager screens, but not here.
        Screen before = context.computeOnClient(client -> Compat.currentScreen());
        context.setScreen(() -> (Screen) reflect(() -> Class.forName("fi.dy.masa.litematica.gui.GuiSchematicSave").getConstructor().newInstance()));
        context.waitTicks(5);
        context.runOnClient(client -> Compat.currentScreen().onFilesDrop(List.of(schematic, text)));
        context.waitTicks(5);
        context.setScreen(() -> before);

        List<String> found = reflect(() -> {
            try (Stream<Path> files = Files.walk(base, 3)) {
                return files.map(path -> path.getFileName().toString()).filter(name -> name.startsWith("cele-drop-test")).toList();
            }
        });
        check(found.stream().anyMatch(name -> name.endsWith(".litematic")), "Schematic Drag & Drop: the dropped .litematic was not copied into " + base);
        check(found.stream().noneMatch(name -> name.endsWith(".txt")), "Schematic Drag & Drop: a dropped .txt was copied: " + found);
    }

    /** Many block entities nearby relax TweakerMore's syncer; once they are gone the player's values return. */
    private static void syncerTune(ClientGameTestContext context, TestServerContext server, BlockPos p) {
        context.runOnClient(client -> {
            SyncerTuneFeature.Config cfg = CeleConfig.get().section(FeatureDefinition.SYNCER_TUNE, SyncerTuneFeature.Config.class);
            cfg.chunkRadius = 1;
            cfg.checkIntervalTicks = 5;
            cfg.enterThreshold = 4;
            cfg.exitThreshold = 2;
        });
        Object interval = context.computeOnClient(client -> reflect(() ->
                Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs").getField("SERVER_DATA_SYNCER_QUERY_INTERVAL").get(null)));
        Object limit = context.computeOnClient(client -> reflect(() ->
                Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs").getField("SERVER_DATA_SYNCER_QUERY_LIMIT").get(null)));
        int ownInterval = intValue(context, interval);
        int ownLimit = intValue(context, limit);
        check(ownInterval != 3 || ownLimit != 128, "setup: TweakerMore's own syncer values already equal the relaxed ones");

        // The feature counts block entities in the client's world, which follows the server some ticks later.
        BlockPos from = p.offset(2, 0, 2);
        BlockPos to = from.offset(5, 0, 0);
        server.runCommand("fill " + xyz(from) + " " + xyz(to) + " minecraft:chest");
        check(eventually(context, client -> integerValue(interval) == 3 && integerValue(limit) == 128),
                "Syncer Tune: six chests did not relax the syncer (interval " + intValue(context, interval) + ", limit " + intValue(context, limit) + ")");

        server.runCommand("fill " + xyz(from) + " " + xyz(to) + " minecraft:air");
        check(eventually(context, client -> integerValue(interval) == ownInterval && integerValue(limit) == ownLimit),
                "Syncer Tune: the player's values were not restored (interval " + intValue(context, interval) + ", limit " + intValue(context, limit) + ")");
    }

    /** TweakerMore's schematic pick does not run while the selected hotbar slot is locked. */
    private static void autoPickScope(ClientGameTestContext context, BlockPos p) {
        Method pick = context.computeOnClient(client -> method("me.fallenbreath.tweakermore.impl.features.schematicProPlace.SchematicBlockPicker",
                "doSchematicWorldPickBlock", net.minecraft.client.Minecraft.class, BlockPos.class, InteractionHand.class));
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));

        setLockedSlots(context, "1");
        int before = Spies.SCHEMATIC_PICK.get();
        context.runOnClient(client -> invoke(pick, null, client, p, InteractionHand.MAIN_HAND));
        check(Spies.SCHEMATIC_PICK.get() == before, "Auto Pick Scope: TweakerMore's schematic pick ran on a locked slot");

        setLockedSlots(context, "");
        context.runOnClient(client -> invoke(pick, null, client, p, InteractionHand.MAIN_HAND));
        check(Spies.SCHEMATIC_PICK.get() == before + 1, "control: TweakerMore's schematic pick did not run on an unlocked slot");
    }

    /** With scope "only listed: dirt", Tweakeroo's tool switch runs for dirt and not for stone. */
    private static void toolSwitchScope(ClientGameTestContext context, TestServerContext server, BlockPos p) {
        BlockPos stone = p.offset(-2, 0, 2);
        BlockPos dirt = p.offset(-3, 0, 2);
        server.runCommand("setblock " + xyz(stone) + " minecraft:stone");
        server.runCommand("setblock " + xyz(dirt) + " minecraft:dirt");
        context.runOnClient(client -> {
            ToolSwitchScopeFeature.Config cfg = CeleConfig.get().section(FeatureDefinition.TOOL_SWITCH_SCOPE, ToolSwitchScopeFeature.Config.class);
            cfg.scope = ToolSwitchScopeFeature.Scope.ONLY_LISTED;
            cfg.blocks = new ArrayList<>(List.of("minecraft:dirt"));
            cfg.keepInHand = new ArrayList<>();
        });
        // The scope is judged on the client's copy of the block, which reaches it from the server some ticks later.
        check(eventually(context, client -> client.level.getBlockState(stone).is(Blocks.STONE) && client.level.getBlockState(dirt).is(Blocks.DIRT)),
                "setup: the client never saw the stone at " + xyz(stone) + " and the dirt at " + xyz(dirt));
        Method trySwitch = context.computeOnClient(client -> method("fi.dy.masa.tweakeroo.util.InventoryUtils", "trySwitchToEffectiveTool", BlockPos.class));

        int before = Spies.TOOL_SWITCH.get();
        context.runOnClient(client -> invoke(trySwitch, null, stone));
        int afterStone = Spies.TOOL_SWITCH.get();
        if (afterStone != before) {
            throw new AssertionError("Tool Switch Scope: Tweakeroo's tool switch ran for stone, which is out of scope; " + toolSwitchView(context, stone));
        }
        context.runOnClient(client -> invoke(trySwitch, null, dirt));
        int afterDirt = Spies.TOOL_SWITCH.get();
        if (afterDirt != afterStone + 1) {
            throw new AssertionError("control: Tweakeroo's tool switch ran " + (afterDirt - afterStone)
                    + " time(s) for dirt, which is in scope, instead of once; " + toolSwitchView(context, dirt));
        }
    }

    /** What the scope check saw at {@code pos}, for a failure message. */
    private static String toolSwitchView(ClientGameTestContext context, BlockPos pos) {
        return context.computeOnClient(client -> {
            BlockState state = client.level.getBlockState(pos);
            return "the client sees " + state + " there and Cele-Tweaks " + (ToolSwitchScopeFeature.allowSwitch(state) ? "allows" : "refuses")
                    + " the switch, holding " + client.player.getMainHandItem();
        });
    }

    /** Item Scroller's mass-craft step stays off in the player inventory unless Shift is held. */
    private static void shiftToMassCraft(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("gamemode survival @a"); // the creative inventory replaces the player inventory screen
        context.waitTicks(SETTLE);
        Screen before = context.computeOnClient(client -> Compat.currentScreen());
        Player player = context.computeOnClient(client -> client.player);
        context.setScreen(() -> new InventoryScreen(player));
        context.waitTicks(5);

        int start = Spies.MASS_CRAFT.get();
        context.waitTicks(SETTLE);
        int withoutShift = Spies.MASS_CRAFT.get();
        context.getInput().holdShift();
        context.waitTicks(SETTLE);
        int withShift = Spies.MASS_CRAFT.get();
        context.getInput().releaseShift();
        context.setScreen(() -> before);
        server.runCommand("gamemode creative @a");

        check(withoutShift == start, "Shift to Mass Craft: Item Scroller's mass-craft step ran " + (withoutShift - start) + " time(s) in the inventory without Shift");
        check(withShift > withoutShift, "control: Item Scroller's mass-craft step did not run with Shift held, so the spy saw nothing");
    }

    private static void setStandIns(ClientGameTestContext context, List<String> rules) {
        context.runOnClient(client -> CeleConfig.get().section(FeatureDefinition.PLACEMENT_CONVERSIONS, PlacementConversionsFeature.Config.class).standIns =
                new ArrayList<>(rules));
    }

    private static void setLockedSlots(ClientGameTestContext context, String slots) {
        context.runOnClient(client -> CeleConfig.get().section(FeatureDefinition.HOTBAR_LOCK, HotbarLockFeature.Config.class).lockedSlots = slots);
    }

    private static String interactVerdict(ClientGameTestContext context, Method interact, Item held, BlockState world, BlockState schematic) {
        return context.computeOnClient(client -> {
            client.player.getInventory().setItem(client.player.getInventory().getSelectedSlot(), new ItemStack(held));
            Object result = invoke(interact, null, client.player, world, schematic);
            Method getType = reflect(() -> result.getClass().getMethod("getType"));
            return ((Enum<?>) invoke(getType, result)).name();
        });
    }

    private static int intValue(ClientGameTestContext context, Object option) {
        return context.computeOnClient(client -> integerValue(option));
    }

    /** A malilib integer option's value; client thread only. */
    private static int integerValue(Object option) {
        return (int) invoke(reflect(() -> option.getClass().getMethod("getIntegerValue")), option);
    }

    /**
     * Waits until {@code condition} holds on the client, checking once a tick for up to {@link #TIMEOUT}
     * ticks, and returns whether it did. For state that crosses from the server to the client, where a
     * fixed number of ticks is a guess that can come up short.
     */
    private static boolean eventually(ClientGameTestContext context, Predicate<Minecraft> condition) {
        for (int tick = 0; ; tick++) {
            if (context.computeOnClient(condition::test)) return true;
            if (tick >= TIMEOUT) return false;
            context.waitTick();
        }
    }

    private static Object call(ClientGameTestContext context, Method method, Object... args) {
        return context.computeOnClient(client -> invoke(method, null, args));
    }

    /** Call on the client thread: loading a mod class can run code that needs Minecraft.getInstance(). */
    private static Method method(String owner, String name, Class<?>... parameters) {
        return reflect(() -> {
            Method method = Class.forName(owner).getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        });
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException failure) {
            throw new AssertionError(method.getName() + " threw", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("could not call " + method.getName(), failure);
        }
    }

    private static <T> T reflect(Callable<T> body) {
        try {
            return body.call();
        } catch (Exception failure) {
            throw new AssertionError("integration test setup failed", failure);
        }
    }

    private static boolean loaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }

    private static String xyz(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
