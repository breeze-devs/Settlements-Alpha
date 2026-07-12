package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PinnedSelection;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the {@code fillPolicyFor} gate fix (4d carry-forward): a {@link PlanIntent#ofCarriedPins}
 * intent has non-empty {@code pins()} but empty {@code selections()}. If the gate regresses to
 * {@code intent.isEmpty()} (true only when BOTH are empty), a carried-pins intent would wrongly
 * take the LLM-overlay fill branch, whose per-band pool draws only from {@code selections()} — empty
 * here — collapsing every band's gap-fill to nothing. This test fails under that regression because
 * the SOCIAL fill behaviors would never appear.
 */
class DayPlanComposerCarriedPinsTest {

    private static final int WAKE_CIVIL = TimeOfDay.AT_06_00.getCivilTick();
    // Mirrors DayPlanComposerGapFillTest's NITWIT rest/nitwit MORNING band derivation.
    private static final int BAND_START_CIVIL = WAKE_CIVIL + GameTicks.minutes(40).getTicksAsInt();
    private static final int BAND_END_CIVIL = TimeOfDay.AT_11_00.getCivilTick() - 500;

    private static final int PIN_CIVIL_TICK = TimeOfDay.AT_08_00.getCivilTick();

    private static final BehaviorKey PINNED_COMMITMENT = BehaviorKey.of("carried_pin_commitment");
    private static final BehaviorKey SOCIAL_A = BehaviorKey.of("carried_pin_social_a");
    private static final BehaviorKey SOCIAL_B = BehaviorKey.of("carried_pin_social_b");
    private static final CooldownRange SHORT_COOLDOWN = CooldownRange.ofSeconds(15, 30);

    @Test
    void compose_withCarriedPinIntent_placesThePinAndStillProducesOrdinaryGapFill() {
        // Arrange — no meal anchor table rows, so the only anchor in play is the carried pin itself.
        DayPlanComposer composer = new DayPlanComposer(List.of());
        PlanGenerationContext context = context();
        PinnedSelection carriedPin = new PinnedSelection(PINNED_COMMITMENT, PIN_CIVIL_TICK, false);
        PlanIntent intent = PlanIntent.ofCarriedPins(List.of(carriedPin));

        // Act
        DayPlan plan = composer.compose(context, intent);

        // Assert (i) — the carried pin was placed as a pinned anchor at its requested tick.
        List<PlanSlot> pinnedSlots = plan.getSlots().stream()
                .filter(PlanSlot::isPinned)
                .filter(slot -> slot.getBehaviorKey().equals(PINNED_COMMITMENT))
                .toList();
        assertEquals(1, pinnedSlots.size(), "the carried pin must place exactly one pinned anchor");
        assertEquals(PIN_CIVIL_TICK, pinnedSlots.getFirst().getStartTick(),
                "an uncontested pin must place at its own requested tick");

        // Assert (ii) — ordinary heuristic gap-fill still ran for the band around the pin. This is
        // what a reverted fill-gate fix would zero out (see class javadoc).
        List<PlanSlot> gapFillSlots = plan.getSlots().stream()
                .filter(slot -> !slot.isPinned())
                .filter(slot -> slot.getStartTick() >= BAND_START_CIVIL && slot.getStartTick() < BAND_END_CIVIL)
                .filter(slot -> slot.getBehaviorKey().equals(SOCIAL_A) || slot.getBehaviorKey().equals(SOCIAL_B))
                .toList();
        assertFalse(gapFillSlots.isEmpty(), "the band must still produce ordinary heuristic gap-fill slots, not just the pin");
    }

    private static PlanGenerationContext context() {
        List<WeightedBehavior> behaviors = List.of(
                weighted(SOCIAL_A, 1),
                weighted(SOCIAL_B, 1),
                // Weight 0 so ordinary weighted-random gap-fill never draws it — the only way it can
                // appear in the plan is via the pin's own anchor placement.
                weighted(PINNED_COMMITMENT, 0));
        return PlanGenerationContext.builder()
                .profession(VillagerProfessionKey.NITWIT)
                .genetics(genetics())
                .scheduleProfile(ScheduleProfile.defaultFor(VillagerProfessionKey.NITWIT))
                .restDayPolicy(RestDayPolicy.defaultFor(VillagerProfessionKey.NITWIT))
                .dayType(PlanDayType.WORK_DAY)
                .availableBehaviors(behaviors)
                .wakeAtAbsoluteTick(TimeOfDay.AT_06_00.getMinecraftTick())
                .chronotypeSeed(0L)
                .planSeed(4242L)
                .build();
    }

    private static WeightedBehavior weighted(BehaviorKey key, int weight) {
        return new WeightedBehavior(BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(BehaviorCategory.SOCIAL)
                .intensity(WorkIntensity.NONE)
                .estimatedDuration(GameTicks.minutes(15))
                .preconditionSummary("carried-pin test fixture")
                .cooldown(SHORT_COOLDOWN)
                .build(), weight);
    }

    private static GeneticsProfile genetics() {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        return new GeneticsProfile(genes);
    }

}
