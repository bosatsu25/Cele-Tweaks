package io.github.bosatsu25.celetweaks.feature.look;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SlowLookFeature#scaledSensitivity}: three-value boundary analysis on the factor
 * (0 | 1 ... 100 | 101) and on the result's clamp to the option's 0-1 range.
 */
class SlowLookScaleTest {
    private static final double EPSILON = 1e-9;

    @ParameterizedTest(name = "{0} at {1}% -> {2}")
    @CsvSource({
            "0.5, 0,   0.005",   // below the floor: treated as 1%
            "0.5, 1,   0.005",   // floor
            "0.5, 2,   0.010",
            "0.5, 35,  0.175",   // default
            "0.5, 99,  0.495",
            "0.5, 100, 0.5",     // ceiling: unchanged
            "0.5, 101, 0.5",     // above the ceiling: treated as 100%
            "0.5, -7,  0.005",
    })
    void theFactorIsClampedToOneToOneHundredPercent(double original, int factorPercent, double expected) {
        assertEquals(expected, SlowLookFeature.scaledSensitivity(original, factorPercent), EPSILON);
    }

    @ParameterizedTest(name = "{0} at {1}% -> {2}")
    @CsvSource({
            "1.0, 100, 1.0",   // the option's maximum stays in range
            "0.0, 100, 0.0",   // the option's minimum stays in range
            "0.0, 1,   0.0",
            "1.5, 100, 1.0",   // an out-of-range original is pulled back into 0-1
            "-0.5, 50, 0.0",
    })
    void theResultStaysInsideTheOptionsRange(double original, int factorPercent, double expected) {
        assertEquals(expected, SlowLookFeature.scaledSensitivity(original, factorPercent), EPSILON);
    }
}
