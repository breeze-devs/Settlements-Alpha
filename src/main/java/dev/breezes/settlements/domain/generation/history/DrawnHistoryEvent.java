package dev.breezes.settlements.domain.generation.history;

import org.apache.commons.lang3.Validate;

/**
 * A history event a settlement's generation drew, placed in an era but not yet dated.
 *
 * @param eventId     the id of the history event definition that fired; not blank
 * @param timeHorizon the era it happened in, from its definition's time horizon range; a lower value is an earlier era
 */
public record DrawnHistoryEvent(String eventId, int timeHorizon) {

    public DrawnHistoryEvent {
        Validate.notBlank(eventId, "History event id must not be blank");
    }

}
