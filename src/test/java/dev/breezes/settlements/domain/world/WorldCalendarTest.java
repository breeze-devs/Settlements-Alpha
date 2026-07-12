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
        long justBeforeMidnight = TimeOfDay.AT_00_00.getMinecraftTick() - 1L;

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(justBeforeMidnight);

        // Assert
        assertEquals(0L, calendarDay);
    }

    @Test
    void calendarDayOf_midnightCrossesIntoNextDay() {
        // Arrange
        long midnight = TimeOfDay.AT_00_00.getMinecraftTick();

        // Act
        long calendarDay = WorldCalendar.calendarDayOf(midnight);

        // Assert
        assertEquals(1L, calendarDay);
    }

    @Test
    void calendarDayOf_preDawnHoursBelongToUpcomingCalendarDay() {
        // 04:30 AM the morning after world spawn — calendarwise tomorrow, MC-tickwise still day 0.
        // Arrange
        long farmerWakeMorningAfterSpawn = TimeOfDay.AT_04_30.getMinecraftTick();

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
        long farmerWakeOnDayOne = TimeOfDay.AT_04_30.getMinecraftTick();
        long librarianWakeOnDayOne = TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_08_00.getMinecraftTick();

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
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_08_00.getMinecraftTick());

        // Assert
        assertEquals(TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_08_00.getMinecraftTick(), absolute);
    }

    @Test
    void absoluteTickFor_preDawnTickReturnsTickInPreviousMcDay() {
        // Farmer wake at 04:30 (tick 22 500) on calendar day 1 → dayTime 22 500 (MC day 0, tick 22 500).
        // Arrange, Act
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_04_30.getMinecraftTick());

        // Assert
        assertEquals((long) TimeOfDay.AT_04_30.getMinecraftTick(), absolute);
    }

    @Test
    void absoluteTickFor_midnightTickReturnsTickInPreviousMcDay() {
        // Midnight (tick 18 000) on calendar day 1 is the boundary between day 0 and day 1;
        // its absolute dayTime is in MC day 0 at tick 18 000.
        // Arrange, Act
        long absolute = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_00_00.getMinecraftTick());

        // Assert
        assertEquals((long) TimeOfDay.AT_00_00.getMinecraftTick(), absolute);
    }

    @Test
    void absoluteTickFor_roundTripsThroughCalendarDayOf() {
        // For any (calendarDay, tickInMcDay) pair, encoding then decoding must return the same calendar day.
        // Sweeps both pre-dawn and post-dawn ticks across several days.
        // Arrange
        long[] calendarDays = {0L, 1L, 2L, 5L, 17L};
        int[] ticksInMcDay = {
                0,
                TimeOfDay.AT_06_30.getMinecraftTick(),
                TimeOfDay.AT_12_00.getMinecraftTick(),
                TimeOfDay.AT_17_30.getMinecraftTick(),
                TimeOfDay.AT_00_00.getMinecraftTick(),
                TimeOfDay.AT_04_30.getMinecraftTick(),
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

    @Test
    void absoluteTickForCivil_civilMidnightAgreesWithAbsoluteTickForOnMcMidnightTick() {
        // Both helpers must land on the same absolute instant for civil tick 0 == MC tick 18 000
        // (the two representations of "midnight").
        // Arrange, Act
        long viaCivil = WorldCalendar.absoluteTickForCivil(1L, 0);
        long viaMcTick = WorldCalendar.absoluteTickFor(1L, TimeOfDay.AT_00_00.getMinecraftTick());

        // Assert
        assertEquals(viaMcTick, viaCivil);
    }

    @Test
    void absoluteTickForCivil_agreesWithAbsoluteTickForAcrossTheDay() {
        // For every named time of day, absoluteTickForCivil(civil) must agree with
        // absoluteTickFor(mc) — the two are the same instant expressed in different tick spaces.
        // Arrange, Act, Assert
        for (TimeOfDay timeOfDay : TimeOfDay.values()) {
            long viaCivil = WorldCalendar.absoluteTickForCivil(3L, timeOfDay.getCivilTick());
            long viaMcTick = WorldCalendar.absoluteTickFor(3L, timeOfDay.getMinecraftTick());
            assertEquals(viaMcTick, viaCivil, "mismatch for " + timeOfDay);
        }
    }

    @Test
    void civilOffsetWithin_zeroAtOwnMidnight() {
        // Arrange, Act
        int offset = WorldCalendar.civilOffsetWithin(2L, WorldCalendar.absoluteTickForCivil(2L, 0));

        // Assert
        assertEquals(0, offset);
    }

    @Test
    void civilOffsetWithin_matchesCivilTickPartwayThroughTheDay() {
        // Arrange
        long dayTime = WorldCalendar.absoluteTickForCivil(2L, TimeOfDay.AT_12_00.getCivilTick());

        // Act
        int offset = WorldCalendar.civilOffsetWithin(2L, dayTime);

        // Assert
        assertEquals(TimeOfDay.AT_12_00.getCivilTick(), offset);
    }

    @Test
    void civilOffsetWithin_isUnboundedPastTheFollowingMidnight() {
        // A dayTime a full day past calendarDay's midnight must NOT wrap back into
        // [0, TICKS_PER_DAY) — it must read as "past everything" for plan comparisons.
        // Arrange
        long dayTime = WorldCalendar.absoluteTickForCivil(2L, 0) + TimeOfDay.TICKS_PER_DAY + 500;

        // Act
        int offset = WorldCalendar.civilOffsetWithin(2L, dayTime);

        // Assert
        assertEquals(TimeOfDay.TICKS_PER_DAY + 500, offset);
    }

    @Test
    void civilOffsetWithin_isNegativeBeforeCalendarDayBegins() {
        // A dayTime still on the PRECEDING calendar day must read as negative, not wrap forward.
        // Arrange
        long dayTime = WorldCalendar.absoluteTickForCivil(2L, 0) - 750;

        // Act
        int offset = WorldCalendar.civilOffsetWithin(2L, dayTime);

        // Assert
        assertEquals(-750, offset);
    }

}
