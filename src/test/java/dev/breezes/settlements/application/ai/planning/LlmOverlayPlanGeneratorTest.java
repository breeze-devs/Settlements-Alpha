package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.inference.plan.PlanSelection;
import dev.breezes.settlements.application.ai.inference.plan.VillagerPlanResult;
import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minecraft-free unit tests for {@link LlmOverlayPlanGenerator}.
 * <p>
 * Mirrors {@link HeuristicPlanGeneratorTest}'s context/behavior construction — no Minecraft
 * objects are built anywhere in this class.
 */
class LlmOverlayPlanGeneratorTest {

    private final DayPlanComposer composer = new DayPlanComposer(DefaultMealAnchorTable.rows());
    private final HeuristicPlanGenerator heuristicGenerator = new HeuristicPlanGenerator(this.composer);
    private final LlmOverlayPlanGenerator overlayGenerator = new LlmOverlayPlanGenerator(this.composer);

    @Test
    void generate_singleBehaviorSelectionYieldsOnlyThatBehaviorInWindow() {
        // Arrange: the MORNING (work-day) window's pool has two eligible work behaviors, but the
        // selection names only one. A naive "pack the full pool with the ordered strategy" would
        // still spill HARVEST_SUGARCANE in as coverage-first filler — this is the #1 trap.
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.MORNING, List.of(BehaviorKey.MILK_COW.id())));

        // Act
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert
        List<BehaviorKey> nonMealKeys = nonMealKeys(plan);
        assertFalse(nonMealKeys.isEmpty(), "the selected behavior must still be packed");
        assertTrue(nonMealKeys.stream().allMatch(BehaviorKey.MILK_COW::equals),
                "only the selected behavior may appear — HARVEST_SUGARCANE must not spill in as coverage filler");
    }

    @Test
    void generate_afternoonOrderReflectsSelectionOrderNotPoolOrder() {
        // Arrange: pool order is [a, b] but the selection order is [b, a] — the earliest packed
        // slot must follow the selection, proving ordering is driven by the LLM's list, not the
        // window's pool iteration order or weight.
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.of("a"), BehaviorCategory.SOCIAL, WorkIntensity.NONE),
                weighted(BehaviorKey.of("b"), BehaviorCategory.SOCIAL, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.AFTERNOON, List.of("b", "a")));

        // Act
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert — DayPlan already returns slots sorted in chronological (linear) order.
        List<PlanSlot> nonMealSlots = plan.getSlots().stream()
                .filter(slot -> !slot.getBehaviorKey().equals(BehaviorKey.EAT_FOOD))
                .toList();
        assertFalse(nonMealSlots.isEmpty());
        assertEquals(BehaviorKey.of("b"), nonMealSlots.get(0).getBehaviorKey(),
                "earliest packed slot must honor the LLM order (b before a)");
    }

    @Test
    void generate_omittedWindowYieldsNoSlotsForThatWindow() {
        // Arrange: MILK_COW is LIGHT work, so it would normally be eligible afternoon filler too —
        // but AFTERNOON is omitted entirely, so it must contribute nothing, not just "less."
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.MORNING, List.of(BehaviorKey.MILK_COW.id())));

        // Act
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert
        List<BehaviorKey> nonMealKeys = nonMealKeys(plan);
        assertTrue(nonMealKeys.stream().allMatch(BehaviorKey.MILK_COW::equals),
                "an omitted AFTERNOON window must not contribute any behavior slots");
        assertFalse(plan.getSchedule().activityBlocks().isEmpty(), "scaffold schedule must still be present");
    }

    @Test
    void generate_unknownIdIsDroppedWithoutThrowing() {
        // Arrange
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.MORNING,
                List.of("totally_bogus_id", BehaviorKey.MILK_COW.id())));

        // Act — must not throw
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert
        List<BehaviorKey> nonMealKeys = nonMealKeys(plan);
        assertFalse(nonMealKeys.isEmpty());
        assertTrue(nonMealKeys.stream().allMatch(BehaviorKey.MILK_COW::equals));
        assertTrue(nonMealKeys.stream().noneMatch(key -> key.equals(BehaviorKey.of("totally_bogus_id"))));
    }

    @Test
    void generate_scaffoldMatchesHeuristicForSameContext() {
        // Arrange: an empty result — the overlay must still produce the identical deterministic
        // scaffold (meals, schedule, wake/dayStart/dayType) the heuristic produces for this context.
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);

        // Act
        DayPlan heuristicPlan = this.heuristicGenerator.generate(context);
        DayPlan overlayPlan = this.overlayGenerator.generate(context, result(Map.of()));

        // Assert
        assertEquals(heuristicPlan.getDayType(), overlayPlan.getDayType());
        assertEquals(heuristicPlan.getWakeAtAbsoluteTick(), overlayPlan.getWakeAtAbsoluteTick());
        assertEquals(heuristicPlan.getCalendarDay(), overlayPlan.getCalendarDay());
        assertEquals(heuristicPlan.getSchedule(), overlayPlan.getSchedule());

        List<PlanSlot> heuristicMeals = mealSlots(heuristicPlan);
        List<PlanSlot> overlayMeals = mealSlots(overlayPlan);
        assertEquals(heuristicMeals.size(), overlayMeals.size());
        for (int i = 0; i < heuristicMeals.size(); i++) {
            assertEquals(heuristicMeals.get(i).getStartTick(), overlayMeals.get(i).getStartTick());
            assertEquals(heuristicMeals.get(i).getEstimatedDurationTicks(), overlayMeals.get(i).getEstimatedDurationTicks());
            assertEquals(heuristicMeals.get(i).getPriority(), overlayMeals.get(i).getPriority());
        }
    }

    @Test
    void generate_allEmptySelectionsYieldsMealsAndScheduleOnly() {
        // Arrange
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);

        // Act
        DayPlan plan = this.overlayGenerator.generate(context, result(Map.of()));

        // Assert — a "lighter day" floor: meals and schedule survive, nothing else does.
        assertTrue(nonMealKeys(plan).isEmpty());
        assertEquals(3, mealSlots(plan).size());
        assertFalse(plan.getSchedule().activityBlocks().isEmpty());
    }

    @Test
    void generate_selectionMayPlaceBehaviorInWindowThatHeuristicPoolExcludes() {
        // Capability lock: the overlay draws from the FULL available pool, not the heuristic's
        // per-window pool. HARVEST_SUGARCANE is HEAVY work, which the heuristic's AFTERNOON pool
        // (afternoonCandidates: social/light-work/self-care/leisure) deliberately excludes — so
        // sourcing from window.pool() would silently drop it. Windows are pure time buckets to the
        // overlay, so a HEAVY-work behavior placed in AFTERNOON must still pack.
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                weighted(BehaviorKey.of("gossip"), BehaviorCategory.SOCIAL, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.AFTERNOON, List.of(BehaviorKey.HARVEST_SUGARCANE.id())));

        // Act
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert
        assertTrue(nonMealKeys(plan).contains(BehaviorKey.HARVEST_SUGARCANE),
                "a HEAVY-work behavior selected for AFTERNOON must pack even though the heuristic afternoon pool excludes it");
    }

    @Test
    void generate_restDaySelectionOfSuppressedWorkBehaviorIsStillPacked() {
        // Regression lock: on a REST_DAY the MORNING window's base multiplier is restDayMultiplier,
        // which maps a WORK behavior of neither LIGHT nor HEAVY intensity (WORK + NONE) to 0.0 —
        // and WindowPacker filters weight() > 0 BEFORE the strategy runs, so a zeroed candidate is
        // silently dropped. The overlay must NOT inherit that suppression: an explicitly-selected
        // behavior is the villager's stated intent and must pack regardless of rest-day weighting.
        BehaviorKey suppressedWork = BehaviorKey.of("odd_work");
        List<WeightedBehavior> behaviors = List.of(
                weighted(suppressedWork, BehaviorCategory.WORK, WorkIntensity.NONE),
                weighted(BehaviorKey.of("gossip"), BehaviorCategory.SOCIAL, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.REST_DAY, behaviors);
        VillagerPlanResult result = result(Map.of(
                PlanBand.MORNING, List.of(suppressedWork.id())));

        // Act
        DayPlan overlayPlan = this.overlayGenerator.generate(context, result);
        DayPlan heuristicPlan = this.heuristicGenerator.generate(context);

        // Assert — overlay packs the selection despite rest-day suppression...
        assertTrue(nonMealKeys(overlayPlan).contains(suppressedWork),
                "an explicitly-selected WORK+NONE behavior must pack on a rest day, not be zero-dropped");
        // ...whereas the heuristic DOES suppress it, documenting the intended divergence.
        assertFalse(nonMealKeys(heuristicPlan).contains(suppressedWork),
                "the heuristic still suppresses WORK+NONE on a rest day (restDayMultiplier -> 0.0)");
    }

    @Test
    void toIntent_populatesEmptyListPerBandWhenModelSelectsNothing() {
        // Arrange
        List<PlanBand> bands = List.of(band(PlanBand.MORNING), band(PlanBand.AFTERNOON));
        VillagerPlanResult result = result(Map.of());

        // Act
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert — every band the composer derived gets an entry, even when nothing was selected;
        // this is what distinguishes "the model considered every band and chose nothing" from
        // PlanIntent.empty() (the pure-heuristic sentinel).
        assertFalse(intent.isEmpty());
        assertEquals(List.of(), intent.selections().get(PlanBand.MORNING));
        assertEquals(List.of(), intent.selections().get(PlanBand.AFTERNOON));
    }

    @Test
    void toIntent_mapsIdsToDistinctOrderedBehaviorKeysPerBand() {
        // Arrange: a duplicate id and out-of-declaration-order ids
        List<PlanBand> bands = List.of(band(PlanBand.MORNING));
        VillagerPlanResult result = result(Map.of(PlanBand.MORNING, List.of(
                BehaviorKey.MILK_COW.id(), BehaviorKey.HARVEST_SUGARCANE.id(), BehaviorKey.MILK_COW.id())));

        // Act
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert — order preserved, the later duplicate dropped rather than moving MILK_COW's rank
        assertEquals(List.of(BehaviorKey.MILK_COW, BehaviorKey.HARVEST_SUGARCANE),
                intent.selections().get(PlanBand.MORNING));
    }

    @Test
    void toIntent_blankIdSelectionIsSkippedWithoutThrowing() {
        // Arrange: a blank id would reach BehaviorKey.of and throw. Unguarded, that exception unwinds
        // through the gateway's parseLine catch and the entire villager's overlay is dropped for the
        // cycle over one bad entry — the blank must be skipped, leaving the valid selection intact.
        List<PlanBand> bands = List.of(band(PlanBand.MORNING));
        VillagerPlanResult result = resultWithSelections(Map.of(PlanBand.MORNING, List.of(
                PlanSelection.builder().id("   ").build(),
                PlanSelection.builder().id(BehaviorKey.MILK_COW.id()).build())));

        // Act — must not throw
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert
        assertEquals(List.of(BehaviorKey.MILK_COW), intent.selections().get(PlanBand.MORNING));
    }

    @Test
    void toIntent_resultMentioningAnUnderivedBandIsIgnored() {
        // Arrange: the result names a band the composer never derived for this context/day type
        List<PlanBand> bands = List.of(band(PlanBand.MORNING));
        VillagerPlanResult result = result(Map.of("EVENING", List.of(BehaviorKey.MILK_COW.id())));

        // Act
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert — the mapping is driven by the composer's own bands, not by whatever the result mentions
        assertEquals(Set.of(PlanBand.MORNING), intent.selections().keySet());
    }

    @Test
    void toIntent_atBearingSelectionBecomesAPinNotAnOrdinaryFillEntry() {
        // Arrange
        List<PlanBand> bands = List.of(band(PlanBand.MORNING));
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.MORNING, List.of(pinned(BehaviorKey.MILK_COW, TimeOfDay.AT_08_00))));

        // Act
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert — the pin is extracted, not left in the ordinary fill preference list
        assertEquals(List.of(), intent.selections().get(PlanBand.MORNING));
        assertEquals(List.of(new PinnedSelection(BehaviorKey.MILK_COW, TimeOfDay.AT_08_00.getCivilTick(), false)),
                intent.pins());
    }

    @Test
    void toIntent_eatFoodAtBearingSelectionIsFlaggedAsAMealPin() {
        // Arrange
        List<PlanBand> bands = List.of(band(PlanBand.EVENING));
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.EVENING, List.of(pinned(BehaviorKey.EAT_FOOD, TimeOfDay.AT_18_00))));

        // Act
        PlanIntent intent = LlmOverlayPlanGenerator.toIntent(bands, result);

        // Assert
        assertEquals(1, intent.pins().size());
        assertTrue(intent.pins().get(0).meal(), "an eat_food pin must be flagged as a meal");
    }

    @Test
    void generate_atBearingSelectionIsPlacedAsAFixedFlexibleAnchor() {
        // Arrange — a custom, chronotype-independent meal table isolates this test from the
        // frame's chronotype-derived jitter, so the obstacle anchor sits at an exact, known tick.
        BehaviorKey pinKey = BehaviorKey.of("pin_fixed_slot");
        DayPlanComposer customComposer = new DayPlanComposer(List.of(noJitterMealRow(TimeOfDay.AT_08_00, 200)));
        LlmOverlayPlanGenerator generator = new LlmOverlayPlanGenerator(customComposer);
        List<WeightedBehavior> behaviors = List.of(weighted(pinKey, BehaviorCategory.SOCIAL, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.MORNING, List.of(pinned(pinKey, TimeOfDay.AT_10_00))));

        // Act
        DayPlan plan = generator.generate(context, result);

        // Assert — placed exactly at the requested tick, flexible (skippable) but fixed in time,
        // and below meal priority.
        PlanSlot pinSlot = plan.getSlots().stream()
                .filter(slot -> slot.getBehaviorKey().equals(pinKey))
                .findFirst().orElseThrow(() -> new AssertionError("pinned behavior must appear in the plan"));
        assertEquals(TimeOfDay.AT_10_00.getCivilTick(), pinSlot.getStartTick());
        assertTrue(pinSlot.isFlexible(), "a non-meal pin is flexible (skippable) despite its fixed time");
    }

    @Test
    void generate_atCollisionSnapsForwardOneBucketWhenTheSnappedTickIsFree() {
        // Arrange — an obstacle anchor at AT_12_00 collides with the pin's own requested tick; the
        // pin must land exactly one 30-minute bucket later, not at its original request.
        BehaviorKey pinKey = BehaviorKey.of("pin_snap_target");
        DayPlanComposer customComposer = new DayPlanComposer(List.of(noJitterMealRow(TimeOfDay.AT_12_00, 200)));
        LlmOverlayPlanGenerator generator = new LlmOverlayPlanGenerator(customComposer);
        List<WeightedBehavior> behaviors = List.of(weighted(pinKey, BehaviorCategory.SOCIAL, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.MORNING, List.of(pinned(pinKey, TimeOfDay.AT_12_00))));

        // Act
        DayPlan plan = generator.generate(context, result);

        // Assert
        PlanSlot pinSlot = plan.getSlots().stream()
                .filter(slot -> slot.getBehaviorKey().equals(pinKey))
                .findFirst().orElseThrow(() -> new AssertionError("pinned behavior must still be placed after snapping"));
        assertEquals(TimeOfDay.AT_12_00.getCivilTick() + GameTicks.minutes(30).getTicksAsInt(), pinSlot.getStartTick(),
                "a colliding pin must snap forward exactly one 30-minute bucket");
    }

    @Test
    void generate_atCollisionThatSurvivesTheSnapDemotesToOrdinaryFill() {
        // Arrange — two obstacle anchors 30 minutes apart box the pin in on both its requested tick
        // AND its one-bucket snap, so the only legal outcome is demotion to ordinary (unpinned) fill.
        // Boxed at mid-morning (well inside the work band, chronotype-independent) so there is a
        // large guaranteed-free stretch afterward for the demoted key to land in regardless of jitter.
        BehaviorKey pinKey = BehaviorKey.of("pin_demoted");
        DayPlanComposer customComposer = new DayPlanComposer(List.of(
                noJitterMealRow(TimeOfDay.AT_11_00, 200),
                noJitterMealRow(TimeOfDay.AT_11_30, 200)));
        LlmOverlayPlanGenerator generator = new LlmOverlayPlanGenerator(customComposer);
        List<WeightedBehavior> behaviors = List.of(weighted(pinKey, BehaviorCategory.WORK, WorkIntensity.LIGHT));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.MORNING, List.of(pinned(pinKey, TimeOfDay.AT_11_00))));

        // Act
        DayPlan plan = generator.generate(context, result);

        // Assert — the key still shows up (demoted to fill, not dropped), but never at either fixed
        // anchor tick, and with an ordinary band-countdown priority rather than the pin priority band.
        List<PlanSlot> pinKeySlots = plan.getSlots().stream()
                .filter(slot -> slot.getBehaviorKey().equals(pinKey))
                .toList();
        assertFalse(pinKeySlots.isEmpty(), "a demoted pin must still pack via ordinary fill, not be silently dropped");
        for (PlanSlot slot : pinKeySlots) {
            assertTrue(slot.getStartTick() != TimeOfDay.AT_11_00.getCivilTick()
                            && slot.getStartTick() != TimeOfDay.AT_11_30.getCivilTick(),
                    "a demoted pin must not land at either boxed-in fixed tick");
            assertTrue(slot.getPriority() <= 70, "a demoted pin packs at MORNING's own countdown, not the pin priority (80)");
        }
    }

    @Test
    void generate_occasionMealPinSuppressesCollidingDefaultMealAnchor() {
        // Arrange — a default dinner anchor at AT_16_30 collides (within the 60-game-minute window)
        // with an occasion eat_food pin at AT_17_00; the default must be suppressed and the occasion
        // meal placed in its stead.
        DayPlanComposer customComposer = new DayPlanComposer(List.of(noJitterMealRow(TimeOfDay.AT_16_30, 200)));
        LlmOverlayPlanGenerator generator = new LlmOverlayPlanGenerator(customComposer);
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.EAT_FOOD, BehaviorCategory.SELF_CARE, WorkIntensity.NONE));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.EVENING, List.of(pinned(BehaviorKey.EAT_FOOD, TimeOfDay.AT_17_00))));

        // Act
        DayPlan plan = generator.generate(context, result);

        // Assert
        List<PlanSlot> mealSlots = mealSlots(plan);
        assertEquals(1, mealSlots.size(), "the colliding default must be suppressed, leaving only the occasion meal");
        assertEquals(TimeOfDay.AT_17_00.getCivilTick(), mealSlots.get(0).getStartTick());
        assertFalse(mealSlots.get(0).isFlexible(), "an occasion meal pin is rigid, same as a default meal anchor");
    }

    @Test
    void generate_pinWithUnknownIdIsDroppedWithoutThrowing() {
        // Arrange — the pinned id isn't in this villager's available-behaviors pool at all
        List<WeightedBehavior> behaviors = List.of(
                weighted(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY));
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY, behaviors);
        VillagerPlanResult result = resultWithSelections(Map.of(
                PlanBand.MORNING, List.of(pinned(BehaviorKey.of("totally_bogus_pin"), TimeOfDay.AT_08_00))));

        // Act — must not throw
        DayPlan plan = this.overlayGenerator.generate(context, result);

        // Assert
        assertTrue(nonMealKeys(plan).isEmpty());
        assertFalse(plan.getSchedule().activityBlocks().isEmpty());
    }

    /**
     * A meal-table row with no chronotype meal offset, so its placement tick is exactly
     * {@code time.getCivilTick()} regardless of the context's chronotype seed — used to build
     * obstacle anchors at precise, predictable ticks for the pin-placement tests above.
     */
    private static MealAnchorRule noJitterMealRow(TimeOfDay time, int durationTicks) {
        return MealAnchorRule.builder()
                .anchorMode(MealAnchorRule.AnchorMode.FIXED)
                .fixedTime(time)
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(90)
                .durationTicks(durationTicks)
                .authoredKeepProbability(1.0)
                .appliesChronotypeMealOffset(false)
                .build();
    }

    private static PlanBand band(String id) {
        return new PlanBand(id, 0, 1_000, 50, List.of(), PlannerPolicy.DEFAULT_EFFECTIVE_WEIGHT_MULTIPLIER, 1.0);
    }

    private static List<BehaviorKey> nonMealKeys(DayPlan plan) {
        return plan.getSlots().stream()
                .map(PlanSlot::getBehaviorKey)
                .filter(key -> !key.equals(BehaviorKey.EAT_FOOD))
                .toList();
    }

    private static List<PlanSlot> mealSlots(DayPlan plan) {
        return plan.getSlots().stream()
                .filter(slot -> slot.getBehaviorKey().equals(BehaviorKey.EAT_FOOD))
                .toList();
    }

    private static VillagerPlanResult result(Map<String, List<String>> selectionIds) {
        Map<String, List<PlanSelection>> selections = selectionIds.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(id -> PlanSelection.builder().id(id).build()).toList()));
        return VillagerPlanResult.builder()
                .villagerId(UUID.randomUUID())
                .selections(selections)
                .build();
    }

    private static VillagerPlanResult resultWithSelections(Map<String, List<PlanSelection>> selections) {
        return VillagerPlanResult.builder()
                .villagerId(UUID.randomUUID())
                .selections(selections)
                .build();
    }

    private static PlanSelection pinned(BehaviorKey key, TimeOfDay at) {
        return PlanSelection.builder().id(key.id()).at(at.name()).build();
    }

    private static PlanGenerationContext context(VillagerProfessionKey profession, PlanDayType dayType,
                                                 List<WeightedBehavior> behaviors) {
        ScheduleProfile schedule = ScheduleProfile.defaultFor(profession);
        return PlanGenerationContext.builder()
                .profession(profession)
                .genetics(genetics())
                .scheduleProfile(schedule)
                .restDayPolicy(RestDayPolicy.defaultFor(profession))
                .dayType(dayType)
                .availableBehaviors(behaviors)
                .wakeAtAbsoluteTick(schedule.defaultWakeTick())
                .build();
    }

    private static WeightedBehavior weighted(BehaviorKey key, BehaviorCategory category, WorkIntensity intensity) {
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
