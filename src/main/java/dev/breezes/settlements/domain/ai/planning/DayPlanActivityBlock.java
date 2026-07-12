package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;

import javax.annotation.Nonnull;

/**
 * @param startTick start tick in civil time (0 = midnight). Civil time never wraps, so this is always
 *                  strictly less than {@code endTick} within a single authored day.
 */
@Builder
public record DayPlanActivityBlock(
        @Nonnull DayPlanActivityContext context,
        int startTick,
        int endTick
) {

    public DayPlanActivityBlock {
        if (!TimeOfDay.isValidTick(startTick)) {
            throw new IllegalArgumentException("startTick must be between 0 and 23999");
        }
        if (!TimeOfDay.isValidTick(endTick)) {
            throw new IllegalArgumentException("endTick must be between 0 and 23999");
        }
    }

}
