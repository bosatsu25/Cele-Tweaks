package io.github.bosatsu25.celetweaks.testsupport;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;

/**
 * Minecraft 26.2: registers the built-in registries once per JVM so tests can use real blocks,
 * items and item stacks without a running game. Items get an empty component map, the same hack
 * Fabric API's own unit tests use, because item stacks read their item's components on creation.
 */
public final class GameBootstrap {
    private static boolean done;

    private GameBootstrap() {}

    public static synchronized void ensure() {
        if (done) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (Item item : BuiltInRegistries.ITEM) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
        done = true;
    }
}
