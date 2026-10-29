package io.github.bosatsu25.celetweaks.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A compiled list of registry entries written as ids ("minecraft:stone", "stone") or tags
 * ("#minecraft:logs"). Unparsable entries are ignored. Immutable once built.
 */
public final class IdMatcher {
    public static final IdMatcher EMPTY = new IdMatcher(Set.of(), List.of(), List.of(), List.of());

    private final Set<String> ids;
    private final List<TagKey<Block>> blockTags;
    private final List<TagKey<Item>> itemTags;
    private final List<TagKey<EntityType<?>>> entityTags;

    private IdMatcher(Set<String> ids, List<TagKey<Block>> blockTags, List<TagKey<Item>> itemTags,
                      List<TagKey<EntityType<?>>> entityTags) {
        this.ids = ids;
        this.blockTags = blockTags;
        this.itemTags = itemTags;
        this.entityTags = entityTags;
    }

    public static IdMatcher of(Collection<String> entries) {
        if (entries == null || entries.isEmpty()) return EMPTY;
        Set<String> ids = new HashSet<>();
        List<TagKey<Block>> blockTags = new ArrayList<>();
        List<TagKey<Item>> itemTags = new ArrayList<>();
        List<TagKey<EntityType<?>>> entityTags = new ArrayList<>();
        for (String raw : entries) {
            if (raw == null) continue;
            String entry = raw.trim();
            if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag == null) continue;
                blockTags.add(TagKey.create(Registries.BLOCK, tag));
                itemTags.add(TagKey.create(Registries.ITEM, tag));
                entityTags.add(TagKey.create(Registries.ENTITY_TYPE, tag));
            } else {
                String id = Ids.normalize(entry);
                if (id != null) ids.add(id);
            }
        }
        return new IdMatcher(Set.copyOf(ids), List.copyOf(blockTags), List.copyOf(itemTags), List.copyOf(entityTags));
    }

    /** Parses a whitespace separated list, e.g. the right-hand side of a rule. */
    public static IdMatcher parse(String spaceSeparated) {
        if (spaceSeparated == null || spaceSeparated.isBlank()) return EMPTY;
        return of(List.of(spaceSeparated.trim().split("\\s+")));
    }

    public boolean isEmpty() {
        return ids.isEmpty() && blockTags.isEmpty();
    }

    public boolean matches(BlockState state) {
        if (ids.contains(Ids.of(state.getBlock()))) return true;
        for (TagKey<Block> tag : blockTags) if (state.is(tag)) return true;
        return false;
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (ids.contains(Ids.of(stack.getItem()))) return true;
        for (TagKey<Item> tag : itemTags) if (stack.is(tag)) return true;
        return false;
    }

    public boolean matches(EntityType<?> type) {
        if (ids.contains(Ids.of(type))) return true;
        if (entityTags.isEmpty()) return false;
        var holder = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
        for (TagKey<EntityType<?>> tag : entityTags) if (holder.is(tag)) return true;
        return false;
    }
}
