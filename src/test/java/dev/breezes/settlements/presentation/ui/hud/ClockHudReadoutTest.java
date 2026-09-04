package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockHudReadoutTest {

    @Test
    void from_readsPreDawnHoursAsTheDayBeingLivedThroughRatherThanTheOneStillRunningInEngineTerms() {
        // Arrange -- both moments sit inside one Minecraft day, on either side of the midnight between them
        long lateEvening = TimeOfDay.AT_23_00.getMinecraftTick();
        long theSmallHoursThatFollow = TimeOfDay.AT_02_00.getMinecraftTick();

        // Act
        ClockHudReadout evening = ClockHudReadout.from(lateEvening);
        ClockHudReadout smallHours = ClockHudReadout.from(theSmallHoursThatFollow);

        // Assert
        assertEquals(evening.calendarDay() + 1, smallHours.calendarDay());
    }

    @Test
    void from_keepsAMorningAndTheEveningAfterItOnOneDay() {
        // Arrange
        long morning = TimeOfDay.AT_08_00.getMinecraftTick();
        long thatEvening = TimeOfDay.AT_22_00.getMinecraftTick();

        // Act, Assert
        assertEquals(ClockHudReadout.from(morning).calendarDay(), ClockHudReadout.from(thatEvening).calendarDay());
    }

    @Test
    void from_readsMidnightAsTheStartOfTheClock() {
        // Arrange, Act
        ClockHudReadout readout = ClockHudReadout.from(TimeOfDay.AT_00_00.getMinecraftTick());

        // Assert
        assertEquals(0, readout.civilTick());
    }

    @Test
    void from_readsDawnAsAQuarterOfTheWayThroughTheDay() {
        // Arrange, Act
        ClockHudReadout readout = ClockHudReadout.from(TimeOfDay.AT_06_00.getMinecraftTick());

        // Assert
        assertEquals(TimeOfDay.TICKS_PER_DAY / 4, readout.civilTick());
    }

    @Test
    void from_stillReportsOneDayAndOneTimeOfDayAfterManyDaysHaveElapsed() {
        // Arrange
        long elapsedDays = 1830L;
        long noonOnThatDay = elapsedDays * TimeOfDay.TICKS_PER_DAY + TimeOfDay.AT_12_00.getMinecraftTick();

        // Act
        ClockHudReadout readout = ClockHudReadout.from(noonOnThatDay);

        // Assert
        assertEquals(elapsedDays, readout.calendarDay());
        assertEquals(TimeOfDay.TICKS_PER_DAY / 2, readout.civilTick());
    }

}
