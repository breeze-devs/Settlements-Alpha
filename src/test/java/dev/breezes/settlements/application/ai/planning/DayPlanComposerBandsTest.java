package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for {@link DayPlanComposer#deriveBands}, the band-bounds/pool derivation the
 * composer shares between {@link DayPlanComposer#bands} and {@link DayPlanComposer#compose}.
 * <p>
 * These exercise the producer directly, independent of a full {@code compose()} call, which is
 * already covered end-to-end by {@link HeuristicPlanGeneratorTest} and {@link DayPlanGoldenTest}.
 * <p>
 * {@code lunchBandBoundCivil}/{@code dinnerBandBoundCivil} below are passed as the production
 * constants ({@code AT_11_00}/{@code AT_16_30}) so bound arithmetic mirrors what {@link DayPlanComposer#bands}
 * actually threads through in production.
 */
class DayPlanComposerBandsTest {

    private static final int LUNCH_BAND_BOUND_CIVIL = TimeOfDay.AT_11_00.getCivilTick();
    private static final int DINNER_BAND_BOUND_CIVIL = TimeOfDay.AT_16_30.getCivilTick();

    @Test
    void deriveBands_workDayYieldsMorningAfternoonEveningInOrder() {
        // Arrange — wake=0, workStart=1000, workEnd=12000 (well before dinner), bedtime=20000
        // (well after dusk), so none of the three bands collapse.
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY);
        PlannerPolicy.PlannerPalette palette = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        DayPlanComposer.PlanFrame frame = frame(0, 20_000, 1_000, 12_000);

        // Act
        List<PlanBand> bands = DayPlanComposer.deriveBands(
                context, palette, context.restDayPolicy(), frame, LUNCH_BAND_BOUND_CIVIL, DINNER_BAND_BOUND_CIVIL);

        // Assert
        assertEquals(3, bands.size());
        assertEquals(PlanBand.MORNING, bands.get(0).id());
        assertEquals(70, bands.get(0).basePriority());
        assertEquals(1_000, bands.get(0).startCivil());
        assertEquals(12_000, bands.get(0).endCivil(), "MORNING now runs to the raw workEnd — the truncate-at-lunch cap is gone");
        assertTrue(bands.get(0).pool().stream()
                        .allMatch(behavior -> behavior.descriptor().getCategory() == BehaviorCategory.WORK),
                "Work-day morning band must draw only from WORK-category behaviors");

        assertEquals(PlanBand.AFTERNOON, bands.get(1).id());
        assertEquals(50, bands.get(1).basePriority());
        assertEquals(DINNER_BAND_BOUND_CIVIL, bands.get(1).endCivil());

        assertEquals(PlanBand.EVENING, bands.get(2).id());
        assertEquals(40, bands.get(2).basePriority());
        assertEquals(DINNER_BAND_BOUND_CIVIL, bands.get(2).startCivil());
        assertTrue(bands.get(2).pool().stream()
                        .noneMatch(behavior -> behavior.descriptor().getCategory() == BehaviorCategory.WORK),
                "Evening band must never draw WORK-category behaviors");
    }

    @Test
    void deriveBands_restDayYieldsMorningAfternoonEveningInOrder() {
        // Arrange
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.REST_DAY);
        PlannerPolicy.PlannerPalette palette = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        DayPlanComposer.PlanFrame frame = frame(0, 20_000, 1_000, 12_000);

        // Act
        List<PlanBand> bands = DayPlanComposer.deriveBands(
                context, palette, context.restDayPolicy(), frame, LUNCH_BAND_BOUND_CIVIL, DINNER_BAND_BOUND_CIVIL);

        // Assert
        assertEquals(3, bands.size());
        assertEquals(PlanBand.MORNING, bands.get(0).id());
        assertEquals(55, bands.get(0).basePriority());
        // Rest-day morning draws from the full available pool, not just work behaviors.
        assertFalse(bands.get(0).pool().isEmpty());
        assertTrue(bands.get(0).pool().stream()
                        .anyMatch(behavior -> behavior.descriptor().getCategory() == BehaviorCategory.SOCIAL),
                "Rest-day morning band must include non-work candidates");
        assertEquals(PlanBand.AFTERNOON, bands.get(1).id());
        assertEquals(50, bands.get(1).basePriority());
        assertEquals(PlanBand.EVENING, bands.get(2).id());
        assertEquals(40, bands.get(2).basePriority());
    }

    @Test
    void deriveBands_nitwitOnWorkDayStillUsesRestDayMorningBand() {
        // NITWIT follows the rest-day band branch even on a WORK_DAY, mirroring compose()'s
        // "dayType == REST_DAY || profession == NITWIT" condition.
        // Arrange
        PlanGenerationContext context = context(VillagerProfessionKey.NITWIT, PlanDayType.WORK_DAY);
        PlannerPolicy.PlannerPalette palette = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        DayPlanComposer.PlanFrame frame = frame(0, 20_000, 1_000, 12_000);

        // Act
        List<PlanBand> bands = DayPlanComposer.deriveBands(
                context, palette, context.restDayPolicy(), frame, LUNCH_BAND_BOUND_CIVIL, DINNER_BAND_BOUND_CIVIL);

        // Assert
        assertEquals(PlanBand.MORNING, bands.get(0).id());
        assertEquals(55, bands.get(0).basePriority());
    }

    @Test
    void deriveBands_eveningTooNarrowIsDroppedEntirely() {
        // Arrange — bedtime lands so close to dinner that EVENING's clamped width falls under the
        // minimum slot spacing; the empty-band skip must drop it from the returned list rather than
        // returning a degenerate zero/negative-width band.
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY);
        PlannerPolicy.PlannerPalette palette = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        DayPlanComposer.PlanFrame frame = frame(0, DINNER_BAND_BOUND_CIVIL + 100, 1_000, 12_000);

        // Act
        List<PlanBand> bands = DayPlanComposer.deriveBands(
                context, palette, context.restDayPolicy(), frame, LUNCH_BAND_BOUND_CIVIL, DINNER_BAND_BOUND_CIVIL);

        // Assert
        assertEquals(List.of(PlanBand.MORNING, PlanBand.AFTERNOON), bands.stream().map(PlanBand::id).toList(),
                "EVENING must be dropped, not returned with an inverted/too-narrow range");
    }

    @Test
    void deriveBands_workEndPastDinnerCollapsesAfternoonAndPushesEveningToWorkEnd() {
        // Arrange — a long, high-WIL work day whose workEnd (17000) lands past the dinner band
        // bound (16500): AFTERNOON must collapse to nothing, and EVENING must start at workEnd,
        // never overlapping the MORNING band that now legitimately runs that late.
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY);
        PlannerPolicy.PlannerPalette palette = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        int lateWorkEndCivil = DINNER_BAND_BOUND_CIVIL + 500;
        DayPlanComposer.PlanFrame frame = frame(0, 20_000, 1_000, lateWorkEndCivil);

        // Act
        List<PlanBand> bands = DayPlanComposer.deriveBands(
                context, palette, context.restDayPolicy(), frame, LUNCH_BAND_BOUND_CIVIL, DINNER_BAND_BOUND_CIVIL);

        // Assert
        assertEquals(List.of(PlanBand.MORNING, PlanBand.EVENING), bands.stream().map(PlanBand::id).toList(),
                "AFTERNOON must collapse to empty when workEnd already passed the dinner bound");
        PlanBand morning = bands.get(0);
        PlanBand evening = bands.get(1);
        assertEquals(lateWorkEndCivil, morning.endCivil());
        assertEquals(lateWorkEndCivil, evening.startCivil(),
                "EVENING must start at max(dinner, workEnd) = workEnd here, never overlapping MORNING");
        assertTrue(evening.startCivil() >= morning.endCivil(), "EVENING must never start before MORNING ends");
    }

    private static DayPlanComposer.PlanFrame frame(int wakeCivil, int bedtimeCivil, int workStartCivil, int workEndCivil) {
        return DayPlanComposer.PlanFrame.builder()
                .wakeCivil(wakeCivil)
                .bedtimeCivil(bedtimeCivil)
                .workStartCivil(workStartCivil)
                .workEndCivil(workEndCivil)
                .mealOffsetTicks(0)
                .build();
    }

    private static PlanGenerationContext context(VillagerProfessionKey profession, PlanDayType dayType) {
        ScheduleProfile schedule = ScheduleProfile.defaultFor(profession);
        return PlanGenerationContext.builder()
                .profession(profession)
                .genetics(genetics())
                .scheduleProfile(schedule)
                .restDayPolicy(RestDayPolicy.defaultFor(profession))
                .dayType(dayType)
                .availableBehaviors(List.of(
                        descriptor(BehaviorKey.of("gossip"), BehaviorCategory.SOCIAL, WorkIntensity.NONE),
                        descriptor(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY)
                ))
                .wakeAtAbsoluteTick(schedule.defaultWakeTick())
                .build();
    }

    private static WeightedBehavior descriptor(BehaviorKey key, BehaviorCategory category, WorkIntensity intensity) {
        return new WeightedBehavior(BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(category)
                .intensity(intensity)
                .estimatedDuration(GameTicks.minutes(20))
                .preconditionSummary("test")
                .build(), 1);
    }

    private static GeneticsProfile genetics() {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        return new GeneticsProfile(genes);
    }

}
