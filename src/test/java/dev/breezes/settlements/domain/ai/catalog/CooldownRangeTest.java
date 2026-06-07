package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownRangeTest {

    @Test
    void ofSeconds_convertsToServerTicksCorrectly() {
        // Arrange
        ClockTicks expected = ClockTicks.seconds(120);

        // Act
        CooldownRange range = CooldownRange.ofSeconds(120, 120);

        // Assert
        assertEquals(expected.getTicksAsInt(), range.getMin().getTicksAsInt());
        assertEquals(expected.getTicksAsInt(), range.getMax().getTicksAsInt());
    }

    @Test
    void ofSeconds_120SecondsMaps2400Ticks() {
        // 120 real seconds × 20 ticks/second = 2400 ticks
        // Arrange / Act
        CooldownRange range = CooldownRange.ofSeconds(120, 240);

        // Assert
        assertEquals(2400, range.getMin().getTicksAsInt());
        assertEquals(4800, range.getMax().getTicksAsInt());
    }

    @Test
    void drawTicks_withMinEqualsMax_returnsExactValue() {
        // Degenerate range must return the fixed value without throwing
        // Arrange
        CooldownRange range = CooldownRange.ofSeconds(60, 60);
        int expected = ClockTicks.seconds(60).getTicksAsInt();

        // Act
        int result = range.drawTicks();

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void drawTicks_returnsValuesWithinInclusiveBoundsOverManyIterations() {
        // Probabilistic coverage: with 500 draws the likelihood of never hitting a boundary
        // is negligible for a range of meaningful width.
        // Arrange
        int minSeconds = 60;
        int maxSeconds = 240;
        CooldownRange range = CooldownRange.ofSeconds(minSeconds, maxSeconds);
        int minTicks = ClockTicks.seconds(minSeconds).getTicksAsInt();
        int maxTicks = ClockTicks.seconds(maxSeconds).getTicksAsInt();

        // Act / Assert
        for (int i = 0; i < 500; i++) {
            int drawn = range.drawTicks();
            assertTrue(drawn >= minTicks,
                    () -> "drawTicks() returned " + drawn + " which is below min " + minTicks);
            assertTrue(drawn <= maxTicks,
                    () -> "drawTicks() returned " + drawn + " which is above max " + maxTicks);
        }
    }

    @Test
    void drawTicks_withSingleTickRange_returnsCorrectValue() {
        // min == max == 1 tick (edge case for smallest possible non-zero range)
        // Arrange
        CooldownRange range = CooldownRange.builder()
                .min(ClockTicks.ONE)
                .max(ClockTicks.ONE)
                .build();

        // Act
        int result = range.drawTicks();

        // Assert
        assertEquals(1, result);
    }

}
