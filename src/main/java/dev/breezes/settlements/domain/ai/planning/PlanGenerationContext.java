package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.world.WorldCalendar;
import lombok.Builder;

import java.util.List;
import java.util.Set;

/**
 * Immutable input bundle supplied to {@link IPlanGenerator#generate} when constructing a plan.
 * <p>
 * Contains everything a generator needs to produce a contextually appropriate {@link DayPlan}.
 * Values must be immutable or detached snapshots because async generators may consume this context
 * away from Minecraft's server thread.
 *
 * @param chronotypeSeed              Stable per-villager seed for deterministic chronotype offsets.
 *                                    Consumed by the generator to jitter sleep and meal anchors per villager.
 * @param planSeed                    Seeds the single {@link java.util.Random} the generator constructs to
 *                                    drive every plan-path draw. Scoped to (villager, calendar day) so
 *                                    regenerating the same villager's plan on the same day reproduces the
 *                                    identical draw sequence, while different villagers or different days diverge.
 * @param behaviorsLackingOpportunity Behavior keys whose declared opportunity requirements are not currently
 *                                    satisfied. The heuristic planner down-weights these by
 *                                    {@code LOW_OPPORTUNITY_MULTIPLIER} rather than removing them entirely,
 *                                    so the pool's resilience backlog is preserved.
 *                                    Defaults to an empty set when not supplied (e.g. by older tests).
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
        long planSeed,
        Set<BehaviorKey> behaviorsLackingOpportunity
) {

    public PlanGenerationContext {
        availableBehaviors = List.copyOf(availableBehaviors);
        behaviorsLackingOpportunity = behaviorsLackingOpportunity == null
                ? Set.of()
                : Set.copyOf(behaviorsLackingOpportunity);
    }

    /**
     * Derived from {@link #wakeAtAbsoluteTick} so the two cannot drift apart.
     */
    public long calendarDay() {
        return WorldCalendar.calendarDayOf(this.wakeAtAbsoluteTick);
    }

}
