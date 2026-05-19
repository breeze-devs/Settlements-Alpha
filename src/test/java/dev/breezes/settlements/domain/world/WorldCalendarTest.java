package dev.breezes.settlements.domain.world;

import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldCalendarTest {

    @Test
    void calendarDayOf_worldSpawnIsDayZero() {
        // Arrange, Act
        long calendarDay = WorldCalendar.calendarDayOf(0L);

        // Assert
        assertEquals(0L, calendarDay);
    }

    @Test
    void calendarDayOf_lastTickBeforeMidnightIsStillSameDay() {
        // Arrange
        long justBeforeMidnight = TimeOfDay.AT_00_00.getTick() - 1L;

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(justBeforeMidnight);

        // Assert
        assertEquals(0L, calendarDay);
    }

    @Test
    void calendarDayOf_midnightCrossesIntoNextDay() {
        // Arrange
        long midnight = TimeOfDay.AT_00_00.getTick();

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(midnight);

        // Assert
        assertEquals(1L, calendarDay);
    }

    @Test
    void calendarDayOf_preDawnHoursBelongToUpcomingCalendarDay() {
        // 04:30 AM the morning after world spawn — calendarwise tomorrow, MC-tickwise still day 0.
        // Arrange
        long farmerWakeMorningAfterSpawn = TimeOfDay.AT_04_30.getTick();

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(farmerWakeMorningAfterSpawn);

        // Assert
        assertEquals(1L, calendarDay);
    }

    @Test
    void calendarDayOf_dawnOfSecondMcDayIsCalendarDayOne() {
        // Arrange
        long dawnSecondMcDay = TimeOfDay.TICKS_PER_DAY;

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(dawnSecondMcDay);

        // Assert
        assertEquals(1L, calendarDay);
    }

    @Test
    void calendarDayOf_earlyWakeAndStandardWakeOnSameMorningShareTheSameCalendarDay() {
        // Both ticks correspond to "the morning of calendar day 1": the farmer at 04:30 and the
        // librarian at 08:00. Their absolute dayTimes differ by MC-day position but share the day.
        // Arrange
        long farmerWakeOnDayOne = TimeOfDay.AT_04_30.getTick();
        long librarianWakeOnDayOne = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_08_00.getTick();

        // Act
        long farmerDay = WorldCalendar.calendarDayOf(farmerWakeOnDayOne);
        long librarianDay = WorldCalendar.calendarDayOf(librarianWakeOnDayOne);

        // Assert
        assertEquals(farmerDay, librarianDay);
        assertEquals(1L, farmerDay);
    }

    @Test
    void absoluteTickFor_postDawnTickReturnsTickInSameMcDay() {
        // Librarian wake at 08:00 (tick 2 000) on calendar day 1 → dayTime 26 000 (MC day 1, tick 2 000).
        // Arrange, Act
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_08_00.getTick());

        // Assert
        assertEquals(TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_08_00.getTick(), absolute);
    }

    @Test
    void absoluteTickFor_preDawnTickReturnsTickInPreviousMcDay() {
        // Farmer wake at 04:30 (tick 22 500) on calendar day 1 → dayTime 22 500 (MC day 0, tick 22 500).
        // Arrange, Act
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_04_30.getTick());

        // Assert
        assertEquals((long) TimeOfDay.AT_04_30.getTick(), absolute);
    }

    @Test
    void absoluteTickFor_midnightTickReturnsTickInPreviousMcDay() {
        // Midnight (tick 18 000) on calendar day 1 is the boundary between day 0 and day 1;
        // its absolute dayTime is in MC day 0 at tick 18 000.
        // Arrange, Act
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_00_00.getTick());

        // Assert
        assertEquals((long) TimeOfDay.AT_00_00.getTick(), absolute);
    }

    @Test
    void absoluteTickFor_roundTripsThroughCalendarDayOf() {
        // For any (calendarDay, tickInMcDay) pair, encoding then decoding must return the same calendar day.
        // Sweeps both pre-dawn and post-dawn ticks across several days.
        // Arrange
        long[] calendarDays = {0L, 1L, 2L, 5L, 17L};
        int[] ticksInMcDay = {
                0,
                TimeOfDay.AT_06_30.getTick(),
                TimeOfDay.AT_12_00.getTick(),
                TimeOfDay.AT_17_30.getTick(),
                TimeOfDay.AT_00_00.getTick(),
                TimeOfDay.AT_04_30.getTick(),
                TimeOfDay.TICKS_PER_DAY - 1
        };

        // Act, Assert
        for (long day : calendarDays) {
            for (int tick : ticksInMcDay) {
                long absolute = WorldCalendar.absoluteTickFor(day, tick);
                assertEquals(day, WorldCalendar.calendarDayOf(absolute),
                        "round-trip failed for calendarDay=" + day + ", tickInMcDay=" + tick);
            }
        }
    }

}
