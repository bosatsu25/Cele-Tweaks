package io.github.bosatsu25.celetweaks.gametest;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import io.github.bosatsu25.celetweaks.gametest.mixin.MinecraftInvoker;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Checks that the vanilla guards stop what they should and nothing else. Each case performs the action
 * through the client call that the player's input ends in (attack, use, pick block), aimed explicitly
 * rather than through the mouse and camera so the outcome does not depend on timing, once where the
 * guard applies and once as a control where it must not, and reads the outcome back from the server.
 * <p>
 * Nothing here waits a fixed number of ticks. The guards judge the client's copy of the world, so a case
 * starts only once the client has seen the server's blocks; and because the client sends its actions to
 * the server in order, a case checks the guarded outcome only after the control's outcome has arrived,
 * which proves the guarded action (had it been sent) would have arrived too. A control that fails means
 * the test's own action did nothing, so a guard can never pass by accident.
 */
public final class CeleGuardGameTest implements FabricClientGameTest {
    /** Ticks to wait for a server-side outcome before a case fails. */
    private static final int TIMEOUT = 100;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (FeatureDefinition guard : List.of(FeatureDefinition.PORTAL_FRAME_PROTECT, FeatureDefinition.PORTAL_DOORWAY_CLEAR,
                    FeatureDefinition.NO_DIG_UNDERFOOT, FeatureDefinition.ITEM_USE_GUARD, FeatureDefinition.HOTBAR_LOCK)) {
                CeleConfig.get().setEnabled(guard, true);
            }
            // Cobblestone may only be placed on stone: observable, and unrelated to the portal guards.
            CeleConfig.get().section(FeatureDefinition.ITEM_USE_GUARD, ItemUseGuardFeature.Config.class).rules =
                    new ArrayList<>(List.of("minecraft:cobblestone: minecraft:stone"));
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            TestServerContext server = world.getServer();
            // A clear spot north of spawn; every target below is within 4 blocks of the player's eyes.
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition()).offset(0, 0, -10);
            Site site = new Site(p);
            server.runCommand("gamemode creative @a");
            server.runCommand("tp @p " + (p.getX() + 0.5) + " " + p.getY() + " " + (p.getZ() + 0.5));
            site.build(server);
            // The guards read the client's world, so nothing starts before the client shows the site.
            context.waitFor(client -> client.player.isCreative() && client.player.getMainHandItem().is(Items.COBBLESTONE)
                    && site.visibleOn(client));

            portalFrame(context, server, site);
            portalDoorway(context, server, site);
            itemUse(context, server, site);
            hotbarLock(context, server, site);
            noDigUnderfoot(context, server, site);
        }
    }

    /** Fixed layout relative to the player's block {@code p} (frame along X, two blocks south). */
    private record Site(BlockPos p) {
        BlockPos frameBottom() { return p.offset(3, 0, 2); }
        BlockPos looseObsidian() { return p.offset(-2, 0, 2); }
        BlockPos doorwaySupport() { return p.offset(3, 0, 1); }
        BlockPos openStone() { return p.offset(-2, 0, -2); }
        BlockPos ruleStone() { return p.offset(-2, 0, -1); }
        BlockPos ruleDirt() { return p.offset(2, 0, -2); }
        /** A spare spot for the block that proves the client's earlier actions have reached the server. */
        BlockPos marker() { return p.offset(-2, 0, 0); }
        BlockPos underfoot() { return p.below(); }

        void build(TestServerContext server) {
            BlockPos a = p.offset(2, 0, 2);
            BlockPos b = p.offset(5, 4, 2);
            server.runCommand("fill " + xyz(a) + " " + xyz(b) + " minecraft:obsidian");
            server.runCommand("fill " + xyz(a.offset(1, 1, 0)) + " " + xyz(b.offset(-1, -1, 0)) + " minecraft:nether_portal[axis=x]");
            setblock(server, looseObsidian(), "minecraft:obsidian");
            setblock(server, doorwaySupport(), "minecraft:stone");
            setblock(server, openStone(), "minecraft:stone");
            setblock(server, ruleStone(), "minecraft:stone");
            setblock(server, ruleDirt(), "minecraft:dirt");
            server.runCommand("item replace entity @p weapon.mainhand with minecraft:cobblestone 64");
        }

        /** True once the client's world shows every block the cases act on. */
        boolean visibleOn(Minecraft client) {
            return client.level.getBlockState(frameBottom()).is(Blocks.OBSIDIAN)
                    && client.level.getBlockState(frameBottom().above()).is(Blocks.NETHER_PORTAL)
                    && client.level.getBlockState(looseObsidian()).is(Blocks.OBSIDIAN)
                    && client.level.getBlockState(doorwaySupport()).is(Blocks.STONE)
                    && client.level.getBlockState(openStone()).is(Blocks.STONE)
                    && client.level.getBlockState(ruleStone()).is(Blocks.STONE)
                    && client.level.getBlockState(ruleDirt()).is(Blocks.DIRT);
        }
    }

    /** Portal Frame Protection: frame obsidian survives an attack; loose obsidian does not. */
    private static void portalFrame(ClientGameTestContext context, TestServerContext server, Site site) {
        attack(context, site.frameBottom());
        attack(context, site.looseObsidian());
        awaitServer(context, server, s -> blockAt(s, site.looseObsidian()) == Blocks.AIR,
                "control: loose obsidian was not broken, so the attack did nothing");
        check(blockAt(server, site.frameBottom()) == Blocks.OBSIDIAN, "Portal Frame Protection: the portal frame was broken");
        check(blockAt(server, site.frameBottom().above()) == Blocks.NETHER_PORTAL, "Portal Frame Protection: the portal went out");
    }

    /** Clear Portal Doorway: a block cannot go in front of the opening; elsewhere it can. */
    private static void portalDoorway(ClientGameTestContext context, TestServerContext server, Site site) {
        useOnTop(context, site.doorwaySupport());
        useOnTop(context, site.openStone());
        awaitServer(context, server, s -> blockAt(s, site.openStone().above()) == Blocks.COBBLESTONE,
                "control: cobblestone was not placed on open ground");
        check(blockAt(server, site.doorwaySupport().above()) == Blocks.AIR, "Clear Portal Doorway: a block was placed in the doorway");
    }

    /** Item Use Guard: the rule allows cobblestone on stone only. */
    private static void itemUse(ClientGameTestContext context, TestServerContext server, Site site) {
        useOnTop(context, site.ruleDirt());
        useOnTop(context, site.ruleStone());
        awaitServer(context, server, s -> blockAt(s, site.ruleStone().above()) == Blocks.COBBLESTONE,
                "control: cobblestone was not placed on stone, which the rule allows");
        check(blockAt(server, site.ruleDirt().above()) == Blocks.AIR, "Item Use Guard: cobblestone was placed on dirt against the rule");
    }

    /**
     * Hotbar Lock: with every hotbar slot full and the locked slot selected, vanilla Pick Block would
     * overwrite that slot. Locked, the pick is refused; unlocked, the same pick overwrites it. Pick Block
     * changes the client's inventory on the spot, so the locked case is read on the client right after
     * the pick, and the control is read on the server once the pick has reached it.
     */
    private static void hotbarLock(ClientGameTestContext context, TestServerContext server, Site site) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with minecraft:diamond");
        for (int slot = 1; slot < 9; slot++) server.runCommand("item replace entity @p hotbar." + slot + " with minecraft:stick");
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
        setLockedSlots(context, "1");
        // The pick overwrites the selected slot only when the hotbar is full on the client.
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 0 && clientHotbar(client).equals(fullHotbar()));

        pickBlock(context, site.ruleDirt());
        List<Item> afterLocked = context.computeOnClient(CeleGuardGameTest::clientHotbar);
        check(afterLocked.getFirst() == Items.DIAMOND, "Hotbar Lock: Pick Block overwrote the locked slot; hotbar " + afterLocked);

        setLockedSlots(context, "");
        pickBlock(context, site.ruleDirt());
        List<Item> afterUnlocked = context.computeOnClient(CeleGuardGameTest::clientHotbar);
        check(afterUnlocked.getFirst() == Items.DIRT,
                "control: unlocked Pick Block did not replace the selected slot, so the pick did nothing; hotbar " + afterUnlocked);
        awaitServer(context, server, s -> serverHotbar(s).getFirst() == Items.DIRT,
                "control: the unlocked pick never reached the server");
    }

    /**
     * No Digging Underfoot: the block under a moving player survives; standing still, it breaks. Both
     * attempts hit the same block, so a marker block attacked after the guarded attempt, and seen broken
     * on the server, stands in for the control as proof that the guarded attempt has been delivered.
     */
    private static void noDigUnderfoot(ClientGameTestContext context, TestServerContext server, Site site) {
        Block ground = blockAt(server, site.underfoot());
        check(ground != Blocks.AIR, "setup: nothing under the player");
        setblock(server, site.marker(), "minecraft:stone");
        context.waitFor(client -> client.level.getBlockState(site.marker()).is(Blocks.STONE));

        context.runOnClient(client -> {
            client.player.setDeltaMovement(0.2, 0.0, 0.0);
            client.gameMode.startDestroyBlock(site.underfoot(), Direction.UP);
        });
        attack(context, site.marker());
        awaitServer(context, server, s -> blockAt(s, site.marker()) == Blocks.AIR, "control: the marker block was not broken, so attacks do nothing");
        check(blockAt(server, site.underfoot()) == ground, "No Digging Underfoot: the block under the moving player was broken");

        context.runOnClient(client -> {
            client.player.setDeltaMovement(0.0, 0.0, 0.0);
            client.gameMode.startDestroyBlock(site.underfoot(), Direction.UP);
        });
        awaitServer(context, server, s -> blockAt(s, site.underfoot()) == Blocks.AIR, "control: the block under the standing player was not broken");
    }

    private static void attack(ClientGameTestContext context, BlockPos pos) {
        context.runOnClient(client -> client.gameMode.startDestroyBlock(pos, Direction.UP));
    }

    private static void useOnTop(ClientGameTestContext context, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
    }

    /** What the Pick Block key ends in, with the crosshair set on {@code pos} for that one call. */
    private static void pickBlock(ClientGameTestContext context, BlockPos pos) {
        context.runOnClient(client -> {
            client.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            ((MinecraftInvoker) client).celetweaks$pickBlockOrEntity();
        });
    }

    private static void setLockedSlots(ClientGameTestContext context, String slots) {
        context.runOnClient(client ->
                CeleConfig.get().section(FeatureDefinition.HOTBAR_LOCK, HotbarLockFeature.Config.class).lockedSlots = slots);
    }

    /**
     * Waits, one tick at a time and for up to {@link #TIMEOUT} ticks, until {@code condition} holds on
     * the server; fails with {@code message} otherwise. The outcome of a client action crosses to the
     * server some ticks later, and how many is not fixed.
     */
    private static void awaitServer(ClientGameTestContext context, TestServerContext server, Predicate<MinecraftServer> condition, String message) {
        for (int tick = 0; tick < TIMEOUT; tick++) {
            if (server.computeOnServer(condition::test)) return;
            context.waitTick();
        }
        check(server.computeOnServer(condition::test), message + " (after " + TIMEOUT + " ticks)");
    }

    private static Block blockAt(TestServerContext server, BlockPos pos) {
        return server.computeOnServer(s -> blockAt(s, pos));
    }

    private static Block blockAt(MinecraftServer server, BlockPos pos) {
        return server.overworld().getBlockState(pos).getBlock();
    }

    private static List<Item> serverHotbar(MinecraftServer server) {
        List<Item> items = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) items.add(server.getPlayerList().getPlayers().getFirst().getInventory().getItem(slot).getItem());
        return items;
    }

    private static List<Item> clientHotbar(Minecraft client) {
        List<Item> items = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) items.add(client.player.getInventory().getItem(slot).getItem());
        return items;
    }

    /** Slot 0 a diamond, slots 1-8 sticks: the hotbar the lock case starts from. */
    private static List<Item> fullHotbar() {
        List<Item> items = new ArrayList<>();
        items.add(Items.DIAMOND);
        for (int slot = 1; slot < 9; slot++) items.add(Items.STICK);
        return items;
    }

    private static void setblock(TestServerContext server, BlockPos pos, String block) {
        server.runCommand("setblock " + xyz(pos) + " " + block);
    }

    private static String xyz(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
