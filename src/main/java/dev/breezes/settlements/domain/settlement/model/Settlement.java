package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import lombok.Builder;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The persistent record of one settlement.
 *
 * @param id         the settlement's stable identity, unique within its dimension only
 * @param name       the display name; not blank
 * @param origin     how it came to exist
 * @param bounds     the area it occupies in its dimension
 * @param founding   when it was founded and when its record began
 * @param profile    its founding profile
 * @param history    its history, in the order it happened
 * @param roster     its members
 * @param gatherings the days its members have gathered on
 * @param fading     how long it has been fading, or null when it is not fading
 * @param log        what has happened to it
 */
@Builder(toBuilder = true)
public record Settlement(
        UUID id,
        String name,
        SettlementOrigin origin,
        SettlementBounds bounds,
        SettlementFounding founding,
        SettlementProfile profile,
        List<HistoryEvent> history,
        SettlementRoster roster,
        SettlementGatherings gatherings,
        @Nullable SettlementFading fading,
        SettlementLog log
) {

    public Settlement {
        Validate.notNull(id, "Settlement id must not be null");
        Validate.notBlank(name, "Settlement name must not be blank: %s", id);
        Validate.notNull(origin, "Settlement origin must not be null: %s", id);
        Validate.notNull(bounds, "Settlement bounds must not be null: %s", id);
        Validate.notNull(founding, "Settlement founding must not be null: %s", id);
        Validate.notNull(profile, "Settlement profile must not be null: %s", id);
        Validate.notNull(history, "Settlement history must not be null: %s", id);
        history = List.copyOf(history);
        Validate.notNull(roster, "Settlement roster must not be null: %s", id);
        Validate.notNull(gatherings, "Settlement gatherings must not be null: %s", id);
        Validate.notNull(log, "Settlement log must not be null: %s", id);
    }

}
