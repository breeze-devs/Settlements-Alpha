package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record DayPlanActivityBlock(
        @Nonnull DayPlanActivityContext context,
        /**
         * Start tick in game time (may be greater than endTick if the behavior starts pre-dawn)
         */
        int startTick,
        int endTick,
        @Nonnull String reason
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
