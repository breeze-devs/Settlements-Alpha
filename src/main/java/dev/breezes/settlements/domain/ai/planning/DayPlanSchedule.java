package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public record DayPlanSchedule(
        int wakeTick,
        int bedtimeTick,
        @Singular List<DayPlanActivityBlock> activityBlocks
) {

    public DayPlanSchedule {
        if (!TimeOfDay.isValidTick(wakeTick)) {
            throw new IllegalArgumentException("wakeTick must be between 0 and 23999");
        }
        if (!TimeOfDay.isValidTick(bedtimeTick)) {
            throw new IllegalArgumentException("bedtimeTick must be between 0 and 23999");
        }
    }

}
