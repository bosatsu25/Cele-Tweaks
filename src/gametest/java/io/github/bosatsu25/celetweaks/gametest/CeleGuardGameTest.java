package io.github.bosatsu25.celetweaks.gametest;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.feature.guard.ItemUseGuardFeature;
import io.github.bosatsu25.celetweaks.feature.hotbar.HotbarLockFeature;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the vanilla guards stop what they should and nothing else. Each case performs the action
 * through the client call that the player's input ends in (attack, use, pick block), once where the
 * guard applies and once as a control where it must not, and reads the outcome back from the server.
 * A control that fails means the test's own action did nothing, so a guard can never pass by accident.
 */
public final class CeleGuardGameTest implements FabricClientGameTest {
    /** Ticks for a client action to reach the integrated server and its result to settle. */
    private static final int SETTLE = 10;

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
            context.waitTicks(40);

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
    }

    /** Portal Frame Protection: frame obsidian survives an attack; loose obsidian does not. */
    private static void portalFrame(ClientGameTestContext context, TestServerContext server, Site site) {
        attack(context, site.frameBottom());
        attack(context, site.looseObsidian());
        context.waitTicks(SETTLE);
        check(blockAt(server, site.frameBottom()) == Blocks.OBSIDIAN, "Portal Frame Protection: the portal frame was broken");
        check(blockAt(server, site.frameBottom().above()) == Blocks.NETHER_PORTAL, "Portal Frame Protection: the portal went out");
        check(blockAt(server, site.looseObsidian()) == Blocks.AIR, "control: loose obsidian was not broken, so the attack did nothing");
    }

    /** Clear Portal Doorway: a block cannot go in front of the opening; elsewhere it can. */
    private static void portalDoorway(ClientGameTestContext context, TestServerContext server, Site site) {
        useOnTop(context, site.doorwaySupport());
        useOnTop(context, site.openStone());
        context.waitTicks(SETTLE);
        check(blockAt(server, site.doorwaySupport().above()) == Blocks.AIR, "Clear Portal Doorway: a block was placed in the doorway");
        check(blockAt(server, site.openStone().above()) == Blocks.COBBLESTONE, "control: cobblestone was not placed on open ground");
    }

    /** Item Use Guard: the rule allows cobblestone on stone only. */
    private static void itemUse(ClientGameTestContext context, TestServerContext server, Site site) {
        useOnTop(context, site.ruleDirt());
        useOnTop(context, site.ruleStone());
        context.waitTicks(SETTLE);
        check(blockAt(server, site.ruleDirt().above()) == Blocks.AIR, "Item Use Guard: cobblestone was placed on dirt against the rule");
        check(blockAt(server, site.ruleStone().above()) == Blocks.COBBLESTONE, "control: cobblestone was not placed on stone, which the rule allows");
    }

    /**
     * Hotbar Lock: with every hotbar slot full and the locked slot selected, vanilla Pick Block would
     * overwrite that slot. Locked, the pick is refused; unlocked, the same pick overwrites it.
     */
    private static void hotbarLock(ClientGameTestContext context, TestServerContext server, Site site) {
        server.runCommand("clear @p");
        server.runCommand("item replace entity @p hotbar.0 with minecraft:diamond");
        for (int slot = 1; slot < 9; slot++) server.runCommand("item replace entity @p hotbar." + slot + " with minecraft:stick");
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
        setLockedSlots(context, "1");
        context.getInput().lookAt(site.ruleDirt());
        context.waitTicks(SETTLE);

        context.getInput().pressKey(options -> options.keyPickItem);
        context.waitTicks(SETTLE);
        check(hotbarItem(server, 0) == Items.DIAMOND, "Hotbar Lock: Pick Block overwrote the locked slot");

        setLockedSlots(context, "");
        context.getInput().pressKey(options -> options.keyPickItem);
        context.waitTicks(SETTLE);
        check(hotbarItem(server, 0) == Items.DIRT, "control: unlocked Pick Block did not replace the selected slot, so the pick did nothing");
    }

    /** No Digging Underfoot: the block under a moving player survives; standing still, it breaks. */
    private static void noDigUnderfoot(ClientGameTestContext context, TestServerContext server, Site site) {
        Block ground = blockAt(server, site.underfoot());
        check(ground != Blocks.AIR, "setup: nothing under the player");
        context.runOnClient(client -> {
            client.player.setDeltaMovement(0.2, 0.0, 0.0);
            client.gameMode.startDestroyBlock(site.underfoot(), Direction.UP);
        });
        context.waitTicks(SETTLE);
        check(blockAt(server, site.underfoot()) == ground, "No Digging Underfoot: the block under the moving player was broken");

        context.runOnClient(client -> {
            client.player.setDeltaMovement(0.0, 0.0, 0.0);
            client.gameMode.startDestroyBlock(site.underfoot(), Direction.UP);
        });
        context.waitTicks(SETTLE);
        check(blockAt(server, site.underfoot()) == Blocks.AIR, "control: the block under the standing player was not broken");
    }

    private static void attack(ClientGameTestContext context, BlockPos pos) {
        context.runOnClient(client -> client.gameMode.startDestroyBlock(pos, Direction.UP));
    }

    private static void useOnTop(ClientGameTestContext context, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
    }

    private static void setLockedSlots(ClientGameTestContext context, String slots) {
        context.runOnClient(client ->
                CeleConfig.get().section(FeatureDefinition.HOTBAR_LOCK, HotbarLockFeature.Config.class).lockedSlots = slots);
    }

    private static Block blockAt(TestServerContext server, BlockPos pos) {
        return server.computeOnServer(s -> s.overworld().getBlockState(pos).getBlock());
    }

    private static Item hotbarItem(TestServerContext server, int slot) {
        return server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().getItem(slot).getItem());
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
