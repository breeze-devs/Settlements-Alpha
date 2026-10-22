package dev.breezes.settlements.application.ui.dayplan.model;

import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public record DayPlanSnapshot(
        long dayNumber,
        PlanDayType dayType,
        String currentTime,
        PlanStatus planStatus,
        int villagerEntityId,
        String villagerName,
        @Singular List<DayPlanSlotSnapshot> slots
) {

}
