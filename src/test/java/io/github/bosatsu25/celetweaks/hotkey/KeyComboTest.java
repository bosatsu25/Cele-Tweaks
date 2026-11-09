package io.github.bosatsu25.celetweaks.hotkey;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link KeyCombo#matches}: a combination fires only for exactly its keys. Partitions of the pressed
 * set relative to the combo: equal, superset, subset, disjoint, empty; plus the empty combo, which
 * must never fire. Equality ignores the order the keys were given in.
 */
class KeyComboTest {
    private static final int L = 76;
    private static final int K = 75;
    private static final int X = 88;

    @Test
    void firesForExactlyItsKeys() {
        KeyCombo combo = KeyCombo.of(List.of(L, K));
        assertTrue(combo.matches(Set.of(L, K)));
        assertTrue(combo.matches(Set.of(K, L)));
    }

    @Test
    void doesNotFireWhenMoreKeysAreHeld() {
        assertFalse(KeyCombo.of(List.of(L, K)).matches(Set.of(L, K, X)), "L+K must not fire on L+K+X");
    }

    @Test
    void doesNotFireWhenAKeyIsMissingOrDifferent() {
        KeyCombo combo = KeyCombo.of(List.of(L, K));
        assertFalse(combo.matches(Set.of(L)));
        assertFalse(combo.matches(Set.of(L, X)));
        assertFalse(combo.matches(Set.of()));
    }

    @Test
    void theEmptyComboNeverFires() {
        assertTrue(KeyCombo.NONE.isEmpty());
        assertFalse(KeyCombo.NONE.matches(Set.of()));
        assertFalse(KeyCombo.NONE.matches(Set.of(L)));
    }

    @Test
    void keyOrderDoesNotMatterForEqualityOrHashing() {
        KeyCombo a = new KeyCombo(new int[] {L, K});
        KeyCombo b = new KeyCombo(new int[] {K, L});
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, KeyCombo.of(List.of(L)));
        assertNotEquals(a, KeyCombo.of(List.of(L, K, X)));
    }

    @Test
    void theConstructorKeepsItsOwnSortedCopy() {
        int[] given = {L, K};
        KeyCombo combo = new KeyCombo(given);
        given[0] = X;
        assertTrue(combo.matches(Set.of(L, K)), "changing the caller's array afterwards changes nothing");
    }
}
