package io.github.bosatsu25.celetweaks.feature.log;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.Ids;
import io.github.bosatsu25.celetweaks.config.Range;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-memory log of the player's own block and entity interactions with game time and dimension.
 * Read with {@code /celetweaks log [page]}; cleared on every world change and never written to disk.
 */
public final class ActionLogFeature extends Feature {
    private record Entry(long gameTime, String dimension, String action, BlockPos pos, String subject) {}

    private static final int PAGE_SIZE = 10;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    public ActionLogFeature() {
        super(FeatureDefinition.ACTION_LOG);
    }

    @Override
    public void init() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide()) add(level, "hit", pos, Ids.of(level.getBlockState(pos).getBlock()));
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide()) {
                add(level, "use", hit.getBlockPos(), Ids.of(player.getItemInHand(hand).getItem())
                        + " -> " + Ids.of(level.getBlockState(hit.getBlockPos()).getBlock()));
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide()) add(level, "entity", entity.blockPosition(), Ids.of(entity.getType()));
            return InteractionResult.PASS;
        });
    }

    private void add(Level level, String action, BlockPos pos, String subject) {
        if (!isEnabled()) return;
        int max = Math.max(10, config(Config.class).capacity);
        synchronized (ENTRIES) {
            ENTRIES.addFirst(new Entry(level.getGameTime(), level.dimension().identifier().getPath(), action, pos.immutable(), subject));
            while (ENTRIES.size() > max) ENTRIES.removeLast();
        }
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    public static void clear() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }

    public static void printPage(int page, Consumer<Component> out) {
        List<Entry> snapshot;
        synchronized (ENTRIES) {
            snapshot = new ArrayList<>(ENTRIES);
        }
        if (snapshot.isEmpty()) {
            out.accept(Component.translatable("celetweaks.action_log.empty"));
            return;
        }
        int pages = (snapshot.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.max(1, Math.min(page, pages));
        out.accept(Component.translatable("celetweaks.action_log.header", current, pages, snapshot.size()));
        long now = snapshot.get(0).gameTime();
        for (int i = (current - 1) * PAGE_SIZE; i < Math.min(snapshot.size(), current * PAGE_SIZE); i++) {
            Entry e = snapshot.get(i);
            long secondsAgo = Math.max(0, (now - e.gameTime()) / 20);
            out.accept(Component.literal(String.format("-%ds [%s] %s %d %d %d  %s",
                    secondsAgo, e.dimension(), e.action(), e.pos().getX(), e.pos().getY(), e.pos().getZ(), e.subject())));
        }
    }

    public static boolean enabled() {
        return CeleConfig.get().isEnabled(FeatureDefinition.ACTION_LOG);
    }

    public static final class Config {
        @Range(min = 10, max = 1000) public int capacity = 200;
    }
}
