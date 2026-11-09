package io.github.bosatsu25.celetweaks.testsupport;

import io.github.bosatsu25.celetweaks.config.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/**
 * Registry lookups by id for tests. Entity types have no stable constants across 26.x, so a test
 * names the entity by its id and this walks the registry; it needs {@link GameBootstrap#ensure()} first.
 */
public final class Registry {
    private Registry() {}

    public static EntityType<?> entity(String id) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (Ids.of(type).equals(id)) return type;
        }
        throw new AssertionError("no entity type " + id + " in the registry");
    }
}
