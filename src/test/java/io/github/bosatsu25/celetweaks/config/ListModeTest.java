package io.github.bosatsu25.celetweaks.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link ListMode#affects}: the full decision table of mode x listed. */
class ListModeTest {
    @ParameterizedTest(name = "{0}, in list = {1} -> affected = {2}")
    @CsvSource({
            "HIDE,  true,  true",
            "HIDE,  false, false",
            "ALLOW, true,  false",
            "ALLOW, false, true",
    })
    void listedEntriesAreHiddenInHideModeAndSparedInAllowMode(ListMode mode, boolean inList, boolean affected) {
        assertEquals(affected, mode.affects(inList));
    }
}
