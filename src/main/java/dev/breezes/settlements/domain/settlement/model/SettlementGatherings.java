package dev.breezes.settlements.domain.settlement.model;

import org.apache.commons.lang3.Validate;

/**
 * A settlement's tally of member gatherings. The count is not a count of distinct days: rewinding world time can count
 * a calendar day again.
 *
 * @param count   how many gatherings have been counted; non-negative
 * @param lastDay the calendar day of the most recently counted gathering, or NO_DAY exactly when count is zero
 */
public record SettlementGatherings(int count, long lastDay) {

    /**
     * The lastDay of a settlement whose members have never gathered.
     */
    public static final long NO_DAY = -1L;

    public static final SettlementGatherings NONE = new SettlementGatherings(0, NO_DAY);

    public SettlementGatherings {
        Validate.isTrue(count >= 0, "Gathering count must not be negative: %d", count);
        if (count == 0) {
            Validate.isTrue(lastDay == NO_DAY, "A settlement that never gathered has no last gathering day: %d", lastDay);
        } else {
            Validate.isTrue(lastDay >= 0, "Last gathering day must not be negative: %d", lastDay);
        }
    }

    /**
     * Returns the tally after a member's check-in, reusing this instance for consecutive check-ins on the same day.
     * Each change of calendar day counts a new gathering, including when world time moves backward.
     *
     * @param calendarDay the calendar day of the check-in
     * @throws IllegalArgumentException when calendarDay is negative
     */
    public SettlementGatherings checkIn(long calendarDay) {
        // Differs, not later: setting the world's time back rewinds the calendar, and comparing for a later day would
        // stall gatherings until the calendar caught up with lastDay
        if (this.count > 0 && calendarDay == this.lastDay) {
            return this;
        }
        return new SettlementGatherings(this.count + 1, calendarDay);
    }

}
