package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.CivilDateTime;
import lombok.Builder;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One event in a settlement's log.
 *
 * @param date        when the event happened
 * @param type        what happened
 * @param subjectId   the villager the event is about, or null when it concerns the settlement as a whole
 * @param subjectName the subject's name at the time, or null when there is no subject or its name was not known; never
 *                    present without subjectId
 * @param detail      type-specific detail, or null when the event needs none; never blank
 */
@Builder
public record SettlementLogEntry(
        CivilDateTime date,
        SettlementLogType type,
        @Nullable UUID subjectId,
        @Nullable String subjectName,
        @Nullable String detail
) {

    public SettlementLogEntry {
        Validate.notNull(date, "Log entry date must not be null");
        Validate.notNull(type, "Log entry type must not be null");
        Validate.isTrue(subjectName == null || subjectId != null, "Log entry names a subject it does not identify: %s",
                subjectName);
        Validate.isTrue(subjectName == null || !subjectName.isBlank(), "Log entry subject name must not be blank");
        Validate.isTrue(detail == null || !detail.isBlank(), "Log entry detail must not be blank");
    }

}
