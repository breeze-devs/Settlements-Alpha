package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.world.WorldCalendar;
import lombok.Builder;

import java.util.List;

/**
 * Immutable input bundle supplied to {@link IPlanGenerator#generate} when constructing a plan.
 * <p>
 * Contains everything a generator needs to produce a contextually appropriate {@link DayPlan}.
 * Values must be immutable or detached snapshots because async generators may consume this context
 * away from Minecraft's server thread.
 *
 * @param chronotypeSeed             Stable per-villager seed for deterministic chronotype offsets.
 *                                   Consumed by the generator to jitter sleep and meal anchors per villager.
 * @param pendingInvestigateTipCount Number of unverified hearsay tips in the villager's knowledge store
 *                                   at plan-generation time. A value &gt; 0 causes the planner to inject
 *                                   an Investigate scout slot into the morning work block.
 *                                   Defaults to 0 when not supplied by the caller.
 */
@Builder
public record PlanGenerationContext(
        VillagerProfessionKey profession,
        GeneticsProfile genetics,
        ScheduleProfile scheduleProfile,
        RestDayPolicy restDayPolicy,
        PlanDayType dayType,
        List<WeightedBehavior> availableBehaviors,
        long wakeAtAbsoluteTick,
        long chronotypeSeed,
        int pendingInvestigateTipCount
) {

    public PlanGenerationContext {
        availableBehaviors = List.copyOf(availableBehaviors);
    }

    /**
     * Derived from {@link #wakeAtAbsoluteTick} so the two cannot drift apart.
     */
    public long calendarDay() {
        return WorldCalendar.calendarDayOf(this.wakeAtAbsoluteTick);
    }

}
