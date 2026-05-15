package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

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

    public int authoredDayDurationTicks() {
        return Math.floorMod(this.bedtimeTick - this.wakeTick, TICKS_PER_DAY);
    }

}
