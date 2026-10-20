package dev.breezes.settlements.domain.world;

import dev.breezes.settlements.domain.time.GameTicks;

/**
 * A moment on the world's calendar, held as the world's daytime when it happened.
 * Negative when the moment predates the world creation (pre-historic timestamps).
 * <p>
 * Setting the world's time moves the clock without moving stored moments, so after a rewind a stored moment can lie
 * after the current time, and moments stored in sequence need not be in order.
 *
 * @param dayTime the world's daytime at this moment, in ticks
 */
public record CivilDateTime(long dayTime) {

    /**
     * Returns the moment the given duration before this one.
     */
    public CivilDateTime minus(GameTicks duration) {
        return new CivilDateTime(this.dayTime - duration.getTicksAsInt());
    }

    /**
     * Returns whether this moment comes before the other on the timeline.
     */
    public boolean isBefore(CivilDateTime other) {
        return this.dayTime < other.dayTime;
    }

    /**
     * Returns the calendar day this moment falls on.
     */
    public long calendarDay() {
        return WorldCalendar.calendarDayOf(this.dayTime);
    }

}
