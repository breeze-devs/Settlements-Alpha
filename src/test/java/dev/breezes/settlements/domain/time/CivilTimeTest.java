package dev.breezes.settlements.domain.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void civilFromMcTick_negativeMcTickWrapsToSameResultAsItsPositiveEquivalent() {
        // Arrange
        int negativeMcTick = -500;
        int equivalentPositiveMcTick = TimeOfDay.TICKS_PER_DAY - 500;

        // Act
        int civilFromNegative = CivilTime.civilFromMcTick(negativeMcTick);
        int civilFromPositive = CivilTime.civilFromMcTick(equivalentPositiveMcTick);

        // Assert
        assertEquals(civilFromPositive, civilFromNegative);
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

    @Test
    void civilFromDayTime_wrapsNegativeDayTimeBeforeConverting() {
        // A dayTime before world time zero (e.g. from unbounded calendar-day arithmetic) must
        // floor-wrap into [0, TICKS_PER_DAY) rather than produce a negative or truncated tick.
        // Arrange
        long oneCycleBeforeWorldStartAtNoon = -TimeOfDay.TICKS_PER_DAY + 12_000L;

        // Act
        int civilTick = CivilTime.civilFromDayTime(oneCycleBeforeWorldStartAtNoon);

        // Assert
        assertEquals(18_000, civilTick);
    }

    @Test
    void formatClock_midnight_readsAsTheZeroHour() {
        // Catches an hour or minute divisor that offsets the whole clock, which every other
        // reading would then be wrong by too and no single case would isolate.
        // Act, Assert
        assertEquals("00:00", CivilTime.formatClock(0));
    }

    @Test
    void formatClock_lastTickOfTheDay_readsAsTheLastMinuteOfTheLastHour() {
        // Catches a bound that treats the final tick of the day as out of range, and a minute
        // computation that rounds it up into a twenty-fourth hour.
        // Arrange
        long lastTickOfDay = TimeOfDay.TICKS_PER_DAY - 1;

        // Act, Assert
        assertEquals("23:59", CivilTime.formatClock(lastTickOfDay));
    }

    @Test
    void formatClock_lastTickOfAnHour_readsAsMinuteFiftyNineRatherThanSixty() {
        // Catches a minute computation that rounds rather than floors: 999 ticks into the hour is
        // 59.94 minutes, and rounding it would print a minute no clock has.
        // Act, Assert
        assertEquals("00:59", CivilTime.formatClock(999));
        assertEquals("01:00", CivilTime.formatClock(1_000));
    }

    @Test
    void formatClock_tickBeyondTheDay_showsTheTickRatherThanAWrappedTime() {
        // Catches a reintroduced wrap, which would render a corrupt tick as a plausible time of
        // day and hide exactly what this reading exists to expose.
        // Arrange
        long oneDayPastMorning = TimeOfDay.TICKS_PER_DAY + 1_000;

        // Act
        String rendered = CivilTime.formatClock(oneDayPastMorning);

        // Assert
        assertNotEquals(CivilTime.formatClock(1_000), rendered);
        assertTrue(rendered.contains(Long.toString(oneDayPastMorning)));
    }

    @Test
    void formatClock_negativeTick_showsTheTickRatherThanAWrappedTime() {
        // Same wrap, from the other side: a negative civil tick must not read as late evening.
        // Act
        String rendered = CivilTime.formatClock(-1);

        // Assert
        assertNotEquals(CivilTime.formatClock(TimeOfDay.TICKS_PER_DAY - 1), rendered);
        assertTrue(rendered.contains("-1"));
    }

}
