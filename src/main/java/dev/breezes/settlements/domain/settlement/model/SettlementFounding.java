package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.CivilDateTime;
import org.apache.commons.lang3.Validate;

/**
 * When a settlement was founded and when its record began. A settlement is never founded after its record began.
 *
 * @param founded  when it was founded
 * @param recorded when its record began
 */
public record SettlementFounding(CivilDateTime founded, CivilDateTime recorded) {

    public SettlementFounding {
        Validate.notNull(founded, "Founding date must not be null");
        Validate.notNull(recorded, "Record start must not be null");
        Validate.isTrue(!recorded.isBefore(founded), "Invalid timeline: founded (%d) > recorded (%d)",
                founded.dayTime(), recorded.dayTime());
    }

    /**
     * Returns whether the settlement was founded before its record began, so it has a past of its own.
     * A settlement players built is founded as its record begins.
     */
    public boolean isEstablished() {
        return this.founded.isBefore(this.recorded);
    }

}
