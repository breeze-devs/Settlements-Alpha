package dev.breezes.settlements.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomUtilTest {

    @Test
    void stochasticRound_returnsExactIntegerWhenValueHasNoFraction() {
        int result = RandomUtil.stochasticRound(2.0);

        assertEquals(2, result);
    }

    @Test
    void stochasticRound_returnsEitherFloorOrCeilingWhenValueHasFraction() {
        int result = RandomUtil.stochasticRound(2.25);

        assertTrue(result == 2 || result == 3);
    }

    @Test
    void stochasticRound_rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.stochasticRound(-0.1));
    }

}
