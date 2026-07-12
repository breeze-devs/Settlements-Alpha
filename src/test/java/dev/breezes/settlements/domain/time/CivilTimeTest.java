package dev.breezes.settlements.domain.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CivilTimeTest {

    @Test
    void civilFromMcTick_dawnIsSixThousandCivil() {
        // Arrange
        int dawnMcTick = 0;

        // Act
        int civilTick = CivilTime.civilFromMcTick(dawnMcTick);

        // Assert
        assertEquals(6_000, civilTick);
    }

    @Test
    void civilFromMcTick_midnightIsZeroCivil() {
        // Arrange
        int midnightMcTick = 18_000;

        // Act
        int civilTick = CivilTime.civilFromMcTick(midnightMcTick);

        // Assert
        assertEquals(0, civilTick);
    }

    @Test
    void civilFromMcTick_sunsetIsEighteenThousandCivil() {
        // Arrange
        int sunsetMcTick = 12_000;

        // Act
        int civilTick = CivilTime.civilFromMcTick(sunsetMcTick);

        // Assert
        assertEquals(18_000, civilTick);
    }

    @Test
    void mcTickFromCivil_isInverseOfCivilFromMcTick() {
        // Arrange
        int[] mcTicksToTry = {0, 500, 6_000, 12_000, 18_000, 23_500};

        // Act, Assert
        for (int mcTick : mcTicksToTry) {
            int civilTick = CivilTime.civilFromMcTick(mcTick);
            assertEquals(mcTick, CivilTime.mcTickFromCivil(civilTick),
                    "round-trip failed for mcTick=" + mcTick);
        }
    }

    @Test
    void civilFromDayTime_wrapsMultiDayValuesBeforeConverting() {
        // A dayTime beyond one full day (e.g. the second in-game day's dawn) must land on the
        // same civil tick as its same-time-of-day equivalent within a single day.
        // Arrange
        long dayTimeOnSecondDay = TimeOfDay.TICKS_PER_DAY + 12_000L;

        // Act
        int civilTick = CivilTime.civilFromDayTime(dayTimeOnSecondDay);

        // Assert
        assertEquals(18_000, civilTick);
    }

}
