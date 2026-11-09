package io.github.bosatsu25.celetweaks.testsupport;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Minecraft 26.3: registers the built-in registries once per JVM so tests can use real blocks,
 * items and item stacks without a running game. 26.3 builds item components from initializers
 * against the vanilla registry lookup, the same way Fabric API's own unit tests do.
 */
public final class GameBootstrap {
    private static boolean done;

    private GameBootstrap() {}

    public static synchronized void ensure() {
        if (done) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
        done = true;
    }
}
