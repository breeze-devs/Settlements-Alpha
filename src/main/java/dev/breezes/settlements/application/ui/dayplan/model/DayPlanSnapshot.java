package dev.breezes.settlements.application.ui.dayplan.model;

import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import lombok.Builder;
import lombok.Singular;

import javax.annotation.Nonnull;
import java.util.List;

@Builder
public record DayPlanSnapshot(
        long dayNumber,
        @Nonnull PlanDayType dayType,
        @Nonnull String currentTime,
        @Nonnull PlanStatus planStatus,
        int villagerEntityId,
        @Nonnull String villagerName,
        @Singular @Nonnull List<DayPlanSlotSnapshot> slots
) {

}
