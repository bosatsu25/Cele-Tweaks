package io.github.bosatsu25.celetweaks.hotkey;

import io.github.bosatsu25.celetweaks.compat.Compat;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** One or more GLFW key codes that must be held together. Immutable, order-independent. */
public record KeyCombo(int[] keys) {
    public static final KeyCombo NONE = new KeyCombo(new int[0]);

    public KeyCombo {
        int[] sorted = keys.clone();
        Arrays.sort(sorted);
        keys = sorted;
    }

    public static KeyCombo of(Collection<Integer> keys) {
        return new KeyCombo(keys.stream().mapToInt(Integer::intValue).toArray());
    }

    public boolean isEmpty() { return keys.length == 0; }

    /** Fires only when exactly these keys are held, so L+K does not fire on L+K+X. */
    public boolean matches(Set<Integer> pressed) {
        if (keys.length == 0 || pressed.size() != keys.length) return false;
        for (int key : keys) {
            if (!pressed.contains(key)) return false;
        }
        return true;
    }

    public Component displayName() {
        if (keys.length == 0) return Component.translatable("celetweaks.hotkey.none");
        TreeSet<Integer> ordered = new TreeSet<>();
        for (int key : keys) ordered.add(key);
        return Component.literal(ordered.stream()
                .map(key -> Compat.keyName(key).getString())
                .collect(Collectors.joining(" + ")));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeyCombo combo && Arrays.equals(keys, combo.keys);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(keys); }

    @Override
    public String toString() { return Arrays.toString(keys); }
}
