package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * Where a world's dayTime falls on the calendar the settlement keeps.
 *
 * @param calendarDay day ordinal counted from world creation, anchored on midnight
 * @param civilTick   ticks since that day's own midnight, in [0, TICKS_PER_DAY)
 */
@ClientSide
public record ClockHudReadout(long calendarDay, int civilTick) {

    public static ClockHudReadout from(long dayTime) {
        return new ClockHudReadout(WorldCalendar.calendarDayOf(dayTime), CivilTime.civilFromDayTime(dayTime));
    }

}
