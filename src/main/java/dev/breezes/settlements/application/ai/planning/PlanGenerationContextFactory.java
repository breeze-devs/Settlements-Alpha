package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.catalog.BehaviorPoolResolver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@link PlanGenerationContext} snapshot consumed by {@code IPlanGenerator}/
 * {@code IAsyncPlanGenerator}.
 * <p>
 * Extracted from {@link PlanRunner} (formerly its private {@code createGenerationContext}) so the
 * same server-thread-only assembly — pool resolution, opportunity forecasting — is reusable by the
 * PLAN-request assembler without duplicating it: the request sent to SIS must describe the SAME
 * context the day-plan generator would actually consume.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class PlanGenerationContextFactory {

    private final BehaviorPoolResolver behaviorPoolResolver;
    private final IWeekCycleProvider weekCycleProvider;
    private final OpportunityForecaster opportunityForecaster;

    /**
     * Assembles a {@link PlanGenerationContext} for {@code villager}, anchored at
     * {@code wakeAtAbsoluteTick}. The wake tick is a caller-supplied input, not derived here —
     * resolving the NEXT wake tick is a separate concern owned by the caller (see
     * {@code PlanRunner#nextWakeAtAbsoluteTick}).
     * <p>
     * Must run on the server thread: pool resolution and opportunity forecasting both read
     * decaying spatial memory, whose lazy-expiry read is a mutation and unsafe off-thread.
     */
    public PlanGenerationContext create(@Nonnull BaseVillager villager, long wakeAtAbsoluteTick) {
        VillagerProfessionKey professionKey = villager.getProfession();
        long calendarDay = WorldCalendar.calendarDayOf(wakeAtAbsoluteTick);
        PlanDayType dayType = this.weekCycleProvider.getDayType(calendarDay);
        long chronotypeSeed = chronotypeSeedFor(villager);

        // Resolve the pool before the context so the forecaster can evaluate it on this server thread.
        // DecayingSpatialMemory reads (lazy expiry) are not safe off-thread; the async generator
        // receives only the plain Set<BehaviorKey> verdict, never the villager or its brain.
        List<WeightedBehavior> pool = this.behaviorPoolResolver.resolve(professionKey);
        Set<BehaviorKey> lacking = this.opportunityForecaster.forecastLackingOpportunity(villager, pool);

        return PlanGenerationContext.builder()
                .profession(professionKey)
                .genetics(villager.getGenetics().copy())
                .scheduleProfile(ScheduleProfile.defaultFor(professionKey))
                .restDayPolicy(RestDayPolicy.defaultFor(professionKey))
                .dayType(dayType)
                .availableBehaviors(pool)
                .wakeAtAbsoluteTick(wakeAtAbsoluteTick)
                .chronotypeSeed(chronotypeSeed)
                .planSeed(planSeedFor(chronotypeSeed, calendarDay))
                .behaviorsLackingOpportunity(lacking)
                .build();
    }

    /**
     * Derives a stable, per-villager seed from the UUID so chronotype offsets are reproducible
     * across all call sites that need the same wake tick (scheduling and plan adoption must
     * agree). Package-private so {@link PlanRunner#wakeTickFor} — which needs the identical seed
     * for its own wake-tick resolution outside a generation context — shares this single source
     * rather than carrying a second copy.
     */
    static long chronotypeSeedFor(@Nonnull BaseVillager villager) {
        return villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits();
    }

    /**
     * Derives the plan-path RNG seed from the villager's chronotype seed and the calendar day the
     * plan is authored for, so regenerating the same villager's plan for the same day always
     * reproduces the same draw sequence (golden-test contract), while different villagers or
     * different days land in unrelated parts of the RNG's period.
     */
    private static long planSeedFor(long chronotypeSeed, long calendarDay) {
        long mixed = chronotypeSeed ^ (calendarDay * 0x9E3779B97F4A7C15L);
        return avalanche(mixed);
    }

    /**
     * SplitMix64's finalization mix: three shift-xor-multiply rounds that turn any input into a
     * well-distributed 64-bit output with no fixed points.
     */
    private static long avalanche(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

}
