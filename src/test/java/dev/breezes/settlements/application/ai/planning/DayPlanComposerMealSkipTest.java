package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanIntent;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the P3a meal-skip roll: each meal row survives iff {@code random.nextDouble() <
 * authoredKeepProbability}, rolled independently with NO per-day cap. The golden baselines cannot
 * cover the actual drop path — their seeds happen to keep all three default meals, and breakfast's
 * 1.00 always keeps at least one {@code EAT_FOOD} present regardless — so these fixtures drive the
 * two extreme keep probabilities, which are deterministic (a draw in {@code [0,1)} is always
 * {@code < 1.0} and never {@code < 0.0}) and therefore need no pinned seed.
 * <p>
 * Rigid ({@code !flexible}) slots are meal anchors; the fill pool is a single unrelated SOCIAL
 * behavior so no anchor key can leak in from gap-fill.
 */
class DayPlanComposerMealSkipTest {

    private static final BehaviorKey LUNCH_MARKER = BehaviorKey.of("mealskip_lunch");
    private static final BehaviorKey DINNER_MARKER = BehaviorKey.of("mealskip_dinner");
    private static final BehaviorKey FILL_ONLY = BehaviorKey.of("mealskip_fill_only");

    @Test
    void compose_dropsMealRowWithZeroKeepProbability_keepsCertainRow() {
        // Arrange — breakfast is certain (1.00), lunch is impossible (0.00).
        DayPlanComposer composer = new DayPlanComposer(List.of(
                mealRow(BehaviorKey.EAT_FOOD, TimeOfDay.AT_08_00, 1.00),
                mealRow(LUNCH_MARKER, TimeOfDay.AT_12_00, 0.00)));

        // Act
        DayPlan plan = composer.compose(context(101L), PlanIntent.empty());

        // Assert
        List<BehaviorKey> anchorKeys = anchorKeys(plan);
        assertTrue(anchorKeys.contains(BehaviorKey.EAT_FOOD), "a 1.00 keep-probability meal must always survive");
        assertFalse(anchorKeys.contains(LUNCH_MARKER), "a 0.00 keep-probability meal must always be dropped");
    }

    @Test
    void compose_hasNoPerDayCap_everyBelowRollRowDropsIndependently() {
        // Arrange — breakfast certain, BOTH lunch and dinner impossible. If any "keep at least one
        // besides breakfast" cap existed, one marker would survive; there is deliberately none.
        DayPlanComposer composer = new DayPlanComposer(List.of(
                mealRow(BehaviorKey.EAT_FOOD, TimeOfDay.AT_08_00, 1.00),
                mealRow(LUNCH_MARKER, TimeOfDay.AT_12_00, 0.00),
                mealRow(DINNER_MARKER, TimeOfDay.AT_16_30, 0.00)));

        // Act
        DayPlan plan = composer.compose(context(202L), PlanIntent.empty());

        // Assert
        List<BehaviorKey> anchorKeys = anchorKeys(plan);
        assertTrue(anchorKeys.contains(BehaviorKey.EAT_FOOD), "breakfast still survives");
        assertFalse(anchorKeys.contains(LUNCH_MARKER), "lunch dropped");
        assertFalse(anchorKeys.contains(DINNER_MARKER), "dinner dropped — both may drop the same day, no cap");
    }

    private static List<BehaviorKey> anchorKeys(DayPlan plan) {
        return plan.getSlots().stream()
                .filter(slot -> !slot.isFlexible())
                .map(PlanSlot::getBehaviorKey)
                .toList();
    }

    private static MealAnchorRule mealRow(BehaviorKey key, TimeOfDay time, double keepProbability) {
        return MealAnchorRule.builder()
                .anchorMode(MealAnchorRule.AnchorMode.FIXED)
                .fixedTime(time)
                .behaviorKey(key)
                .priority(90)
                .durationTicks(GameTicks.minutes(10).getTicksAsInt())
                .authoredKeepProbability(keepProbability)
                .appliesChronotypeMealOffset(false)
                .build();
    }

    private static PlanGenerationContext context(long planSeed) {
        return PlanGenerationContext.builder()
                .profession(VillagerProfessionKey.FARMER)
                .genetics(genetics())
                .scheduleProfile(ScheduleProfile.defaultFor(VillagerProfessionKey.FARMER))
                .restDayPolicy(RestDayPolicy.defaultFor(VillagerProfessionKey.FARMER))
                .dayType(PlanDayType.WORK_DAY)
                .availableBehaviors(List.of(new WeightedBehavior(BehaviorPlanningMetadata.builder()
                        .key(FILL_ONLY)
                        .displayName(FILL_ONLY.id())
                        .description(FILL_ONLY.id())
                        .category(BehaviorCategory.SOCIAL)
                        .intensity(WorkIntensity.NONE)
                        .estimatedDuration(GameTicks.minutes(5))
                        .preconditionSummary("meal-skip test fixture")
                        .cooldown(CooldownRange.ofSeconds(15, 30))
                        .build(), 1)))
                .wakeAtAbsoluteTick(ScheduleProfile.defaultFor(VillagerProfessionKey.FARMER).defaultWakeTick())
                .chronotypeSeed(11L)
                .planSeed(planSeed)
                .build();
    }

    private static GeneticsProfile genetics() {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        return new GeneticsProfile(genes);
    }

}
