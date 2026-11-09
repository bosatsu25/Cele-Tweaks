package io.github.bosatsu25.celetweaks.feature.hotbar;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link HotbarLockFeature#parseLockedSlots}: the "1 5 9" text people type. Three-value boundary
 * analysis on the slot number (0 | 1 2 ... 8 9 | 10), and partitions of the text: separators of any
 * kind, duplicates, nothing, no digits, a number too large for an int.
 */
class HotbarLockSlotsTest {
    @ParameterizedTest(name = "slot \"{0}\" -> {1}")
    @CsvSource({
            "0,  ''",     // below the first slot
            "1,  0",      // first slot
            "2,  1",
            "8,  7",
            "9,  8",      // last slot
            "10, ''",     // above the last slot
    })
    void onlySlotsOneToNineAreAccepted(String text, String expected) {
        assertEquals(bits(expected), HotbarLockFeature.parseLockedSlots(text));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "'1 5 9',    '0 4 8'",
            "'1,5;9',    '0 4 8'",
            "'  3  ',    '2'",
            "'3 3',      '2'",
            "'1 0 10 9', '0 8'",
            "'9999999999999', ''",
    })
    void anyNonDigitSeparatesAndBadNumbersAreSkipped(String text, String expected) {
        assertEquals(bits(expected), HotbarLockFeature.parseLockedSlots(text));
    }

    @ParameterizedTest(name = "\"{0}\" locks nothing")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "-", "one five nine"})
    void textWithoutSlotsLocksNothing(String text) {
        assertTrue(HotbarLockFeature.parseLockedSlots(text).isEmpty());
    }

    /** "0 4 8" -> a BitSet with those bits; "" -> empty. */
    private static BitSet bits(String zeroBased) {
        BitSet set = new BitSet(9);
        if (zeroBased != null) {
            for (String token : zeroBased.trim().split("\\s+")) {
                if (!token.isEmpty()) set.set(Integer.parseInt(token));
            }
        }
        return set;
    }
}
