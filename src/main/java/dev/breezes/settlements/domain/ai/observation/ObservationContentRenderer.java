package dev.breezes.settlements.domain.ai.observation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;

/**
 * Renders the human-readable {@code content} string for a knowledge entry from its
 * constituent parts (event type, actor, and an optional parenthetical).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObservationContentRenderer {

    /**
     * Renders {@code "<event type> by <actor or unknown>[ (<parenthetical>)]"}.
     *
     * @param eventTypeName the {@code WorldEventType} enum constant name (e.g. {@code "LEATHER_WASHED"})
     * @param actorId       stringified actor UUID, or null/blank when the actor is unknown
     * @param parenthetical optional trailing detail (e.g. event metadata or a failure reason);
     *                      omitted entirely when null or blank
     */
    public static String render(String eventTypeName, @Nullable String actorId, @Nullable String parenthetical) {
        String actorStr = (actorId != null && !actorId.isBlank()) ? actorId : "unknown";
        String suffix = (parenthetical != null && !parenthetical.isBlank()) ? " (" + parenthetical + ")" : "";
        return eventTypeName.toLowerCase().replace("_", " ") + " by " + actorStr + suffix;
    }

}
