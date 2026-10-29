package io.github.bosatsu25.celetweaks.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Registry id helpers shared by list-driven features. */
public final class Ids {
    private Ids() {}

    public static String of(Block block) { return BuiltInRegistries.BLOCK.getKey(block).toString(); }
    public static String of(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    public static String of(EntityType<?> type) { return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(); }

    /** Normalizes user input ("stone" → "minecraft:stone"); returns null for unparsable text. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        Identifier location = Identifier.tryParse(trimmed);
        return location == null ? null : location.toString();
    }

    public static Set<String> normalizedSet(Collection<String> raw) {
        Set<String> result = new HashSet<>();
        for (String entry : raw) {
            String id = normalize(entry);
            if (id != null) result.add(id);
        }
        return result;
    }
}
