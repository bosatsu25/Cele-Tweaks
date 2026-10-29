package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.catalog.FeatureDefinition;
import io.github.bosatsu25.celetweaks.config.CeleConfig;
import io.github.bosatsu25.celetweaks.config.Ids;
import io.github.bosatsu25.celetweaks.runtime.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Substitution groups for Litematica: items in one group may stand in for each other. When Litematica
 * asks for an item the player does not carry, the first group member the player does carry is offered
 * instead. Groups come from the settings, plus optional built-in groups pairing every block that has a
 * "dead_" variant with that variant (coral and coral fans die into their dead form once placed dry).
 */
public final class MaterialSubstitutionFeature extends Feature {
    private static final Map<Item, List<Item>> CACHE = new ConcurrentHashMap<>();
    private static volatile List<String> cachedFrom;
    private static volatile boolean cachedBuiltIn;

    public MaterialSubstitutionFeature() {
        super(FeatureDefinition.MATERIAL_SUBSTITUTION);
    }

    @Override
    public void onToggle(FeatureDefinition definition, boolean enabled) {
        CACHE.clear();
        cachedFrom = null;
    }

    public static ItemStack substitute(ItemStack wanted) {
        if (wanted == null || wanted.isEmpty() || !CeleConfig.get().isEnabled(FeatureDefinition.MATERIAL_SUBSTITUTION)) return wanted;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return wanted;
        Inventory inventory = player.getInventory();
        if (carries(inventory, wanted.getItem())) return wanted;
        for (Item alternative : alternativesFor(wanted.getItem())) {
            if (carries(inventory, alternative)) return new ItemStack(alternative, wanted.getCount());
        }
        return wanted;
    }

    private static List<Item> alternativesFor(Item item) {
        Config cfg = CeleConfig.get().section(FeatureDefinition.MATERIAL_SUBSTITUTION, Config.class);
        if (!cfg.groups.equals(cachedFrom) || cfg.builtInDeadVariants != cachedBuiltIn) {
            CACHE.clear();
            cachedFrom = List.copyOf(cfg.groups);
            cachedBuiltIn = cfg.builtInDeadVariants;
        }
        return CACHE.computeIfAbsent(item, key -> computeAlternatives(key, cfg));
    }

    private static List<Item> computeAlternatives(Item item, Config cfg) {
        Set<Item> result = new LinkedHashSet<>();
        String id = Ids.of(item);
        for (String group : cfg.groups) {
            List<Item> members = new ArrayList<>();
            boolean contains = false;
            for (String token : group.trim().split("\\s+")) {
                String memberId = Ids.normalize(token);
                if (memberId == null) continue;
                Identifier parsed = Identifier.tryParse(memberId);
                if (parsed == null) continue;
                BuiltInRegistries.ITEM.getOptional(parsed).ifPresent(members::add);
                if (memberId.equals(id)) contains = true;
            }
            if (contains) result.addAll(members);
        }
        if (cfg.builtInDeadVariants) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            String path = key.getPath();
            String partner = path.startsWith("dead_") ? path.substring(5) : "dead_" + path;
            BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(key.getNamespace(), partner)).ifPresent(result::add);
        }
        result.remove(item);
        return List.copyOf(result);
    }

    private static boolean carries(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) return true;
        }
        return false;
    }

    public static final class Config {
        /** Each entry is one group of interchangeable item ids separated by spaces. */
        public List<String> groups = new ArrayList<>();
        /** Pair every item that has a "dead_" counterpart with it, in both directions. */
        public boolean builtInDeadVariants = true;
    }
}
