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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DayPlanComposer}'s gap-fill sub-interval routing: a band is packed as one
 * contiguous interval today (no shipped anchor ever lands mid-band), so this test injects a
 * synthetic mid-band meal-table row to prove the routing machinery itself — required by the
 * design even though it is currently dormant in production — behaves correctly.
 */
class DayPlanComposerGapFillTest {

    private static final int WAKE_CIVIL = TimeOfDay.AT_06_00.getCivilTick();
    // NITWIT's rest/nitwit band branch: [wake + 40min, lunch(11:00) - 30min] = [6666, 10500].
    private static final int BAND_START_CIVIL = WAKE_CIVIL + GameTicks.minutes(40).getTicksAsInt();
    private static final int BAND_END_CIVIL = TimeOfDay.AT_11_00.getCivilTick() - 500;

    private static final int ANCHOR_START_CIVIL = TimeOfDay.AT_09_00.getCivilTick();
    private static final int ANCHOR_DURATION_TICKS = 300;
    private static final int ANCHOR_END_CIVIL = ANCHOR_START_CIVIL + ANCHOR_DURATION_TICKS;

    private static final BehaviorKey SOCIAL_A = BehaviorKey.of("gapfill_social_a");
    private static final BehaviorKey SOCIAL_B = BehaviorKey.of("gapfill_social_b");
    private static final CooldownRange SHORT_COOLDOWN = CooldownRange.ofSeconds(15, 30);

    @Test
    void compose_fillsBothSidesOfAMidBandAnchorWithoutOverlappingIt() {
        // Arrange — a meal table with ONE synthetic row landing inside the MORNING band, splitting
        // it into a before-anchor and an after-anchor free sub-interval.
        DayPlanComposer composer = new DayPlanComposer(List.of(midBandAnchorRule()));
        PlanGenerationContext context = context();

        // Act
        DayPlan plan = composer.compose(context, PlanIntent.empty());

        // Assert
        List<PlanSlot> morningBandFillSlots = plan.getSlots().stream()
                .filter(PlanSlot::isFlexible)
                .filter(slot -> slot.getStartTick() >= BAND_START_CIVIL && slot.getStartTick() < BAND_END_CIVIL)
                .toList();
        assertFalse(morningBandFillSlots.isEmpty(), "the band must still produce fill slots around the anchor");

        List<PlanSlot> beforeAnchor = morningBandFillSlots.stream()
                .filter(slot -> slot.getStartTick() + slot.getEstimatedDurationTicks() <= ANCHOR_START_CIVIL)
                .toList();
        List<PlanSlot> afterAnchor = morningBandFillSlots.stream()
                .filter(slot -> slot.getStartTick() >= ANCHOR_END_CIVIL)
                .toList();

        assertFalse(beforeAnchor.isEmpty(), "the free sub-interval before the anchor must be filled");
        assertFalse(afterAnchor.isEmpty(), "the free sub-interval after the anchor must be filled");
        assertTrue(morningBandFillSlots.size() == beforeAnchor.size() + afterAnchor.size(),
                "every fill slot must fall entirely before or entirely after the anchor — none may overlap it");

        // Cadence/spacing invariant: within each side of the anchor (the packer's own per-call
        // guarantee — see WindowPackerTest), no two same-key slots may be closer than the minimum
        // slot spacing.
        assertNoSameKeySlotsCloserThanMinimumSpacing(beforeAnchor);
        assertNoSameKeySlotsCloserThanMinimumSpacing(afterAnchor);
    }

    private static void assertNoSameKeySlotsCloserThanMinimumSpacing(List<PlanSlot> slots) {
        Map<BehaviorKey, List<PlanSlot>> byKey = slots.stream()
                .collect(Collectors.groupingBy(PlanSlot::getBehaviorKey));
        for (List<PlanSlot> keySlots : byKey.values()) {
            List<PlanSlot> ordered = keySlots.stream()
                    .sorted(Comparator.comparingInt(PlanSlot::getStartTick))
                    .toList();
            for (int i = 1; i < ordered.size(); i++) {
                int gap = ordered.get(i).getStartTick() - ordered.get(i - 1).getStartTick();
                assertTrue(gap >= WindowPacker.MINIMUM_SLOT_SPACING_TICKS,
                        "same-key slots must be spaced by at least the minimum slot spacing, got " + gap);
            }
        }
    }

    private static MealAnchorRule midBandAnchorRule() {
        return MealAnchorRule.builder()
                .anchorMode(MealAnchorRule.AnchorMode.FIXED)
                .fixedTime(TimeOfDay.AT_09_00)
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(100)
                .durationTicks(ANCHOR_DURATION_TICKS)
                .authoredKeepProbability(1.0)
                .appliesChronotypeMealOffset(false)
                .build();
    }

    private static PlanGenerationContext context() {
        List<WeightedBehavior> behaviors = List.of(
                weighted(SOCIAL_A), weighted(SOCIAL_B));
        return PlanGenerationContext.builder()
                .profession(VillagerProfessionKey.NITWIT)
                .genetics(genetics())
                .scheduleProfile(ScheduleProfile.defaultFor(VillagerProfessionKey.NITWIT))
                .restDayPolicy(RestDayPolicy.defaultFor(VillagerProfessionKey.NITWIT))
                .dayType(PlanDayType.WORK_DAY)
                .availableBehaviors(behaviors)
                // AT_06_00's Minecraft-space tick (dawn, tick 0) round-trips to WAKE_CIVIL via
                // CivilTime — see the class javadoc's band-bounds derivation.
                .wakeAtAbsoluteTick(TimeOfDay.AT_06_00.getMinecraftTick())
                .chronotypeSeed(0L)
                .planSeed(777L)
                .build();
    }

    private static WeightedBehavior weighted(BehaviorKey key) {
        return new WeightedBehavior(BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(BehaviorCategory.SOCIAL)
                .intensity(WorkIntensity.NONE)
                .estimatedDuration(GameTicks.minutes(5))
                .preconditionSummary("gap-fill test fixture")
                .cooldown(SHORT_COOLDOWN)
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
