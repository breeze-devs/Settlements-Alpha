package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import lombok.Builder;

import java.util.List;

/**
 * Immutable input bundle supplied to {@link IPlanGenerator#generate} when constructing a plan.
 * <p>
 * Contains everything a generator needs to produce a contextually appropriate {@link DayPlan}.
 */
@Builder
public record PlanGenerationContext(
        VillagerProfessionKey profession,
        GeneticsProfile genetics,
        ScheduleProfile scheduleProfile,
        RestDayPolicy restDayPolicy,
        PlanDayType dayType,
        List<WeightedBehavior> availableBehaviors,
        long gameDay
) {
}
