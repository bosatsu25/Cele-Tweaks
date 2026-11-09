package io.github.bosatsu25.celetweaks.feature.integration;

import io.github.bosatsu25.celetweaks.feature.integration.LoadHysteresis.Change;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Syncer Tune's switch as a state machine. States: NORMAL, RELAXED. Events: sample(load) with the
 * guards "load >= enter" and "load < exit", and reset. The table below is the one this test walks:
 * every valid transition, and every invalid one (a sample that must change nothing).
 * <pre>
 *            | load >= enter | exit <= load < enter | load < exit | reset
 *   NORMAL   | RELAXED/RELAX | NORMAL/-             | NORMAL/-    | NORMAL/-
 *   RELAXED  | RELAXED/-     | RELAXED/-            | NORMAL/RESTORE | NORMAL/RESTORE
 * </pre>
 * With enter = 4 and exit = 2, the boundary loads are 1 2 3 and 3 4 5.
 */
class LoadHysteresisTest {
    private static final int ENTER = 4;
    private static final int EXIT = 2;

    @ParameterizedTest(name = "normal, load {0} -> {1}")
    @CsvSource({"3, NONE", "4, RELAX", "5, RELAX", "0, NONE", "1, NONE", "2, NONE"})
    void fromNormalOnlyALoadAtOrAboveEnterRelaxes(int load, Change expected) {
        LoadHysteresis h = new LoadHysteresis();
        assertEquals(expected, h.sample(load, ENTER, EXIT));
        assertEquals(expected == Change.RELAX, h.isRelaxed());
    }

    @ParameterizedTest(name = "relaxed, load {0} -> {1}")
    @CsvSource({"1, RESTORE", "2, NONE", "3, NONE", "4, NONE", "5, NONE", "0, RESTORE"})
    void fromRelaxedOnlyALoadBelowExitRestores(int load, Change expected) {
        LoadHysteresis h = relaxed();
        assertEquals(expected, h.sample(load, ENTER, EXIT));
        assertEquals(expected != Change.RESTORE, h.isRelaxed());
    }

    @Test
    void aLoadBetweenTheThresholdsKeepsWhateverStateItIsIn() {
        LoadHysteresis h = new LoadHysteresis();
        assertEquals(Change.NONE, h.sample(3, ENTER, EXIT));
        assertFalse(h.isRelaxed());
        h.sample(4, ENTER, EXIT);
        assertEquals(Change.NONE, h.sample(3, ENTER, EXIT));
        assertTrue(h.isRelaxed(), "hysteresis: the same load that did not relax does not restore either");
    }

    @Test
    void aFullCycleFiresEachSideEffectOnce() {
        LoadHysteresis h = new LoadHysteresis();
        assertEquals(Change.RELAX, h.sample(10, ENTER, EXIT));
        assertEquals(Change.NONE, h.sample(10, ENTER, EXIT), "staying above enter changes nothing");
        assertEquals(Change.RESTORE, h.sample(0, ENTER, EXIT));
        assertEquals(Change.NONE, h.sample(0, ENTER, EXIT), "staying below exit changes nothing");
        assertEquals(Change.RELAX, h.sample(4, ENTER, EXIT), "and it can relax again");
    }

    @Test
    void resetRestoresOnlyWhenRelaxed() {
        assertEquals(Change.NONE, new LoadHysteresis().reset());
        LoadHysteresis h = relaxed();
        assertEquals(Change.RESTORE, h.reset());
        assertFalse(h.isRelaxed());
        assertEquals(Change.NONE, h.reset(), "a second reset has nothing to restore");
    }

    // --- threshold sanitising: boundary values of the config ---------------------------------------

    @Test
    void anEnterThresholdBelowOneBecomesOne() {
        assertEquals(Change.NONE, new LoadHysteresis().sample(0, 0, 0), "load 0 is below the floor of 1");
        assertEquals(Change.RELAX, new LoadHysteresis().sample(1, 0, 0));
        assertEquals(Change.RELAX, new LoadHysteresis().sample(1, -100, 0));
    }

    @Test
    void anExitThresholdAboveEnterIsLoweredToEnter() {
        LoadHysteresis h = new LoadHysteresis();
        assertEquals(Change.RELAX, h.sample(4, 4, 10));
        assertEquals(Change.NONE, h.sample(4, 4, 10), "at enter it stays relaxed");
        assertEquals(Change.RESTORE, h.sample(3, 4, 10), "below enter (the effective exit) it restores");
    }

    @Test
    void anExitThresholdBelowZeroBecomesZeroSoOnlyResetRestores() {
        LoadHysteresis h = relaxed();
        assertEquals(Change.NONE, h.sample(0, ENTER, -5), "no load is below 0");
        assertTrue(h.isRelaxed());
        assertEquals(Change.RESTORE, h.reset());
    }

    private static LoadHysteresis relaxed() {
        LoadHysteresis h = new LoadHysteresis();
        assertEquals(Change.RELAX, h.sample(ENTER, ENTER, EXIT));
        return h;
    }
}
