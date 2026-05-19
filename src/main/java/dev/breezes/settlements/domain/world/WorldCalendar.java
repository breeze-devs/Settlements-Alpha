package dev.breezes.settlements.domain.world;

import dev.breezes.settlements.domain.time.TimeOfDay;

/**
 * Calendar arithmetic for the Settlements mod's day cycle.
 * <p>
 * A calendar day spans midnight to midnight in real-world terms, regardless of when each
 * villager wakes. Minecraft's tick-zero falls at 06:00 (dawn) rather than midnight, so a
 * plain {@code dayTime / TICKS_PER_DAY} would put pre-dawn moments on the previous day —
 * which desynchronises early-wake professions from the rest of the village. This helper
 * shifts the day boundary onto midnight so that, for example, a farmer waking at 04:30 and
 * a librarian waking at 08:00 on the same morning share the same calendar day.
 */
public final class WorldCalendar {

    /**
     * Distance from midnight (MC tick 18 000) to the next MC-day boundary (MC tick 24 000 == 0).
     * Adding this offset before dividing by {@link TimeOfDay#TICKS_PER_DAY} aligns the integer
     * day boundary on midnight rather than dawn.
     */
    private static final int MIDNIGHT_OFFSET_FROM_DAWN = TimeOfDay.TICKS_PER_DAY - TimeOfDay.AT_00_00.getTick();

    /**
     * Returns the calendar day containing the given absolute {@code dayTime}.
     */
    public static long calendarDayOf(long dayTime) {
        return Math.floorDiv(dayTime + MIDNIGHT_OFFSET_FROM_DAWN, TimeOfDay.TICKS_PER_DAY);
    }

    /**
     * Returns the absolute {@code dayTime} at which an in-MC-day tick of {@code tickInMcDay}
     * occurs within the given {@code calendarDay}.
     * <p>
     * Pre-dawn ticks (>= midnight) sit in the trailing hours of the previous MC day, so their
     * absolute tick is in MC day {@code calendarDay - 1}. Post-dawn ticks sit in MC day
     * {@code calendarDay}. This is the inverse of {@link #calendarDayOf(long)}.
     */
    public static long absoluteTickFor(long calendarDay, int tickInMcDay) {
        long mcDay = tickInMcDay >= TimeOfDay.AT_00_00.getTick() ? calendarDay - 1 : calendarDay;
        return mcDay * TimeOfDay.TICKS_PER_DAY + tickInMcDay;
    }

}
