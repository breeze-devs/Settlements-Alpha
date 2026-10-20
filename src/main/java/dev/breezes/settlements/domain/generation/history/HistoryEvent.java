package dev.breezes.settlements.domain.generation.history;

import dev.breezes.settlements.domain.world.CivilDateTime;
import org.apache.commons.lang3.Validate;

/**
 * An event in a settlement's history, dated on the world's calendar.
 *
 * @param eventId the id of the history event definition that fired; not blank
 * @param date    when it happened
 */
public record HistoryEvent(String eventId, CivilDateTime date) {

    public HistoryEvent {
        Validate.notBlank(eventId, "History event id must not be blank");
        Validate.notNull(date, "History event date must not be null: %s", eventId);
    }

}
