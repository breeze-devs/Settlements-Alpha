package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.planning.Chronotype;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PinnedSelection;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanIntent;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Single composition pipeline for a villager's day plan.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class DayPlanComposer {

    private static final int LUNCH_BAND_BOUND_CIVIL_TICK = TimeOfDay.AT_11_00.getCivilTick();
    private static final int DINNER_BAND_BOUND_CIVIL_TICK = TimeOfDay.AT_16_30.getCivilTick();
    private static final int AFTERNOON_NOMINAL_START_CIVIL_TICK = TimeOfDay.AT_13_00.getCivilTick();
    private static final int DUSK_CIVIL_TICK = TimeOfDay.AT_18_30.getCivilTick();
    private static final int EVENING_MARGIN_TICKS = GameTicks.minutes(30).getTicksAsInt();
    private static final int BAND_EDGE_MARGIN_TICKS = GameTicks.minutes(30).getTicksAsInt();

    // Over-packing configurations
    private static final double WORK_DAY_MORNING_PACK_FACTOR = 0.75;
    private static final double DEFAULT_PACK_FACTOR = 0.55;

    // Pinned-anchor configuration
    private static final int MEAL_COLLISION_WINDOW_TICKS = GameTicks.minutes(30).getTicksAsInt();
    private static final int PIN_SNAP_BUCKET_TICKS = GameTicks.minutes(30).getTicksAsInt();
    private static final int MEAL_PIN_PRIORITY = 90;
    // "Priority just under meals": below every meal tier (90-100), above the highest band basePriority (70).
    private static final int NON_MEAL_PIN_PRIORITY = 80;

    // Stateless; shared so the common heuristic path allocates nothing extra per band.
    private static final SlotSelectionStrategy HEURISTIC_STRATEGY = new WeightedRandomSelectionStrategy();

    private final List<MealAnchorRule> mealAnchorTable;

    /**
     * Produces the SAME bands {@link #compose} packs into.
     * <p>
     * Convenient for callers that need the day's band shape without generating a full plan.
     */
    public List<PlanBand> bands(PlanGenerationContext context) {
        return deriveBandsForFrame(context, computeFrame(context));
    }

    /**
     * Builds a complete {@link DayPlan} in three stages: frame (wake/sleep/work bounds, no RNG),
     * anchors, then gap-fill (each band's free sub-intervals packed through {@code intent}'s fill policy).
     */
    public DayPlan compose(PlanGenerationContext context, PlanIntent intent) {
        Random random = new Random(context.planSeed());

        PlanFrame frame = computeFrame(context);
        List<PlanBand> bands = deriveBandsForFrame(context, frame);
        AnchorPlacement anchorPlacement = this.computeAnchors(frame, bands, intent, context, random);

        List<PlanSlot> slots = new ArrayList<>();
        for (PlanAnchor anchor : anchorPlacement.anchors()) {
            slots.add(anchor.toSlot());
        }

        for (PlanBand band : bands) {
            this.fillBand(slots, band, intent, context, anchorPlacement, random);
        }

        DayPlanSchedule schedule = buildActivitySchedule(context, frame);

        return DayPlan.builder()
                .slots(slots)
                .dayType(context.dayType())
                .calendarDay(context.calendarDay())
                .schedule(schedule)
                .build();
    }

    private static List<PlanBand> deriveBandsForFrame(PlanGenerationContext context, PlanFrame frame) {
        PlannerPolicy.PlannerPalette policy = new PlannerPolicy.PlannerPalette(context.availableBehaviors());
        return deriveBands(context, policy, context.restDayPolicy(), frame,
                LUNCH_BAND_BOUND_CIVIL_TICK, DINNER_BAND_BOUND_CIVIL_TICK);
    }

    /**
     * Precomputed day-anchor bound math shared between band derivation, anchor placement, and the activity schedule.
     */
    @Builder
    public record PlanFrame(int wakeCivil,
                            int bedtimeCivil,
                            int workStartCivil,
                            int workEndCivil,
                            int mealOffsetTicks) {
    }

    private static PlanFrame computeFrame(PlanGenerationContext context) {
        ScheduleProfile schedule = context.scheduleProfile();

        // Wake arrives pre-baked from the wake-tick resolver
        int wakeCivil = CivilTime.civilFromDayTime(context.wakeAtAbsoluteTick());

        // Compute the chronotype once and reuse for both the sleep offset and the meal offset below.
        Chronotype chronotype = Chronotype.of(context.chronotypeSeed());

        // Sleep shifts with the same chronotype offset, so early birds sleep early and night owls sleep late.
        int bedtimeCivil = CivilTime.civilFromMcTick(schedule.defaultSleepTick() + chronotype.wakeSleepOffsetTicks());

        return PlanFrame.builder()
                .wakeCivil(wakeCivil)
                .bedtimeCivil(bedtimeCivil)
                .workStartCivil(resolveWorkStartCivil(schedule, wakeCivil, bedtimeCivil))
                .workEndCivil(CivilTime.civilFromMcTick(computeWorkEndTick(schedule, context.genetics(), context.dayType())))
                .mealOffsetTicks(chronotype.mealOffsetTicks())
                .build();
    }

    /**
     * Reifies the per-day-type band-bounds/pool/priority/multiplier math so both {@link #bands} and
     * {@link #compose} share one derivation. Performs no packing itself — {@link #fillBand} applies
     * a per-band fill policy to each returned band, which folds in the opportunity multiplier uniformly.
     * <p>
     * MORNING, AFTERNOON, and EVENING are derived in that order as an ordered, non-overlapping
     * partition: each later band's start is clamped to at least the "effective end" of the one
     * before it ({@code max(bandStart, bandEnd)}, not just {@code bandEnd} — that also covers the
     * degenerate case where the earlier band's own bounds already inverted). A long-day (high-WIL)
     * {@code workEnd} that lands past dinner therefore collapses AFTERNOON to zero width and pushes
     * EVENING's start out to {@code workEnd} instead of {@code dinner} — never an overlap. A band
     * whose final width is under {@link WindowPacker#MINIMUM_SLOT_SPACING_TICKS} (inverted or merely
     * too narrow) is dropped from the returned list entirely, which is what makes the empty-band
     * case invisible to every {@link #bands} consumer (wire windows, the LLM overlay's intent map)
     * without their needing their own empty-band handling.
     *
     * @param frame                the composed frame; supplies wake/bedtime/work-start/work-end
     * @param lunchBandBoundCivil  the un-offset lunch anchor tick, in civil ticks — mod-owned band
     *                             math, independent of the (LLM-adjustable) meal anchor table itself
     * @param dinnerBandBoundCivil the un-offset dinner anchor tick, in civil ticks (same independence)
     */
    @VisibleForTesting
    static List<PlanBand> deriveBands(PlanGenerationContext context,
                                      PlannerPolicy.PlannerPalette palette,
                                      RestDayPolicy restDayPolicy,
                                      PlanFrame frame,
                                      int lunchBandBoundCivil,
                                      int dinnerBandBoundCivil) {
        boolean restDayOrNitwit = context.dayType() == PlanDayType.REST_DAY || context.profession().equals(VillagerProfessionKey.NITWIT);
        PlannerPolicy.EffectiveWeightMultiplier restDayWeights = behavior -> PlannerPolicy.restDayMultiplier(behavior.descriptor(), restDayPolicy);
        PlannerPolicy.EffectiveWeightMultiplier finalWeights = context.dayType() == PlanDayType.REST_DAY
                ? restDayWeights
                : PlannerPolicy.DEFAULT_EFFECTIVE_WEIGHT_MULTIPLIER;

        // Morning
        int morningStartCivil;
        int morningEndCivil;
        PlanBand morning;
        if (restDayOrNitwit) {
            morningStartCivil = frame.wakeCivil() + GameTicks.minutes(40).getTicksAsInt();
            // Rest/nitwit MORNING ends near the lunch anchor rather than following work bounds
            morningEndCivil = lunchBandBoundCivil - BAND_EDGE_MARGIN_TICKS;
            morning = new PlanBand(PlanBand.MORNING, morningStartCivil, morningEndCivil, 55,
                    palette.availableBehaviors(), restDayWeights, DEFAULT_PACK_FACTOR);
        } else {
            morningStartCivil = frame.workStartCivil();
            morningEndCivil = frame.workEndCivil();
            morning = new PlanBand(PlanBand.MORNING, morningStartCivil, morningEndCivil, 70,
                    palette.workBehaviors(), PlannerPolicy.DEFAULT_EFFECTIVE_WEIGHT_MULTIPLIER, WORK_DAY_MORNING_PACK_FACTOR);
        }
        int morningEffectiveEnd = Math.max(morningStartCivil, morningEndCivil);

        // TODO: A SOCIAL weight multiplier seeded from CHA is the planned follow-up (context-aware weight extension).
        List<WeightedBehavior> afternoonCandidates = palette.afternoonCandidates(context.genetics());

        // Afternoon
        int afternoonNominalStartCivil = restDayOrNitwit ? restDayMeetPivotCivil(frame.bedtimeCivil()) : AFTERNOON_NOMINAL_START_CIVIL_TICK;
        int afternoonStartCivil = Math.max(morningEffectiveEnd, afternoonNominalStartCivil);
        int afternoonEndCivil = dinnerBandBoundCivil;
        PlanBand afternoon = new PlanBand(PlanBand.AFTERNOON, afternoonStartCivil, afternoonEndCivil, 50, afternoonCandidates, finalWeights, DEFAULT_PACK_FACTOR);
        int afternoonEffectiveEnd = Math.max(afternoonStartCivil, afternoonEndCivil);

        // Evening
        List<WeightedBehavior> eveningCandidates = palette.eveningCandidates();

        int eveningStartCivil = afternoonEffectiveEnd;
        int eveningEndCivil = Math.min(frame.bedtimeCivil() - EVENING_MARGIN_TICKS, DUSK_CIVIL_TICK);
        PlanBand evening = new PlanBand(PlanBand.EVENING, eveningStartCivil, eveningEndCivil, 40, eveningCandidates, finalWeights, DEFAULT_PACK_FACTOR);

        List<PlanBand> bands = new ArrayList<>(3);
        for (PlanBand band : List.of(morning, afternoon, evening)) {
            if (band.endCivil() - band.startCivil() >= WindowPacker.MINIMUM_SLOT_SPACING_TICKS) {
                bands.add(band);
            }
        }
        return bands;
    }

    /**
     * A single fixed-time reservation placed before gap-fill runs.
     */
    private record PlanAnchor(int startTick,
                              BehaviorKey behaviorKey,
                              int priority,
                              int durationTicks,
                              boolean flexible,
                              boolean pinned) {

        public PlanSlot toSlot() {
            return PlanSlot.builder()
                    .startTick(this.startTick)
                    .behaviorKey(this.behaviorKey)
                    .priority(this.priority)
                    .flexible(this.flexible)
                    .estimatedDurationTicks(this.durationTicks)
                    .pinned(this.pinned)
                    .build();
        }

    }

    /**
     * Output of the anchor stage: the merged anchor list, the pin-seeded packer eligibility baseline
     * every fill {@code pack} call copies from, and any pin that failed to place and was demoted back
     * into ordinary per-band fill preference.
     */
    private record AnchorPlacement(List<PlanAnchor> anchors,
                                   Map<BehaviorKey, Integer> pinnedEligibility,
                                   Map<String, List<BehaviorKey>> demotedSelections) {
    }

    /**
     * Places every meal-table row against the frame, then merges in the intent's pins (if any).
     * <p>
     * No clamp rule on the default meal anchors: an anchor whose interval would run past bedtime
     * stays a dead slot.
     * <p>
     * When {@code intent} carries no pins, this returns immediately after the meal roll —
     * the ONLY draws this stage makes on that path are the meal-skip rolls above, in table order.
     */
    private AnchorPlacement computeAnchors(PlanFrame frame,
                                           List<PlanBand> bands,
                                           PlanIntent intent,
                                           PlanGenerationContext context,
                                           Random random) {
        List<PlanAnchor> anchors = new ArrayList<>(this.rollDefaultMealAnchors(frame, random));

        List<PinnedSelection> pins = intent.pins();
        if (pins.isEmpty()) {
            return new AnchorPlacement(anchors, Map.of(), Map.of());
        }

        // An eat_food occasion pin suppresses any surviving default meal anchor it collides with
        for (PinnedSelection pin : pins) {
            if (pin.meal()) {
                anchors.removeIf(anchor -> anchor.behaviorKey().equals(BehaviorKey.EAT_FOOD)
                        && Math.abs(anchor.startTick() - pin.civilTick()) <= MEAL_COLLISION_WINDOW_TICKS);
            }
        }

        Map<BehaviorKey, Integer> pinnedEligibility = new HashMap<>();
        Map<String, List<BehaviorKey>> demotedSelections = new LinkedHashMap<>();

        // Earlier pin takes precedent over later ones
        for (PinnedSelection pin : pins) {
            WeightedBehavior behavior = findBehavior(context, pin.key());
            if (behavior == null) {
                log.error("Unknown pin ID: {}", pin.key());
                continue;
            }

            int duration = behavior.descriptor().getEstimatedDuration().getTicksAsInt();
            int placementTick = resolvePinTick(pin.civilTick(), duration, anchors, frame);
            if (placementTick < 0) {
                String bandId = findBandId(bands, pin.civilTick());
                if (bandId != null) {
                    demotedSelections.computeIfAbsent(bandId, key -> new ArrayList<>()).add(pin.key());
                }
                continue;
            }

            int priority = pin.meal() ? MEAL_PIN_PRIORITY : NON_MEAL_PIN_PRIORITY;
            // Occasion meal pins regenerate as ordinary default meal anchors on a reset
            anchors.add(new PlanAnchor(placementTick, pin.key(), priority, duration, !pin.meal(), !pin.meal()));

            // Cross-pass cadence: seed the fill pass's eligibility floor so no same-key fill slot
            // starts inside this pin's cooldown window (WindowPacker#pack's map overload).
            int cooldownSpacing = Math.max(WindowPacker.MINIMUM_SLOT_SPACING_TICKS, behavior.descriptor().getCooldown().drawTicks(random));
            pinnedEligibility.put(pin.key(), placementTick + cooldownSpacing);
        }

        return new AnchorPlacement(anchors, pinnedEligibility, demotedSelections);
    }

    private List<PlanAnchor> rollDefaultMealAnchors(PlanFrame frame, Random random) {
        List<PlanAnchor> anchors = new ArrayList<>();
        for (MealAnchorRule rule : this.mealAnchorTable) {
            boolean survives = random.nextDouble() < rule.authoredKeepProbability();
            if (!survives) {
                continue;
            }

            int placementTick = rule.placementTick(frame.wakeCivil(), frame.mealOffsetTicks());
            // Default meal anchors always regenerate fresh from the table, so they are never pinned
            anchors.add(new PlanAnchor(placementTick, rule.behaviorKey(), rule.priority(), rule.durationTicks(), false, false));
        }
        return anchors;
    }

    /**
     * Conflict rule: try the pin's own tick, else snap forward one 30-minute bucket, else demote to ordinary fill.
     */
    private static int resolvePinTick(int desiredTick, int duration, List<PlanAnchor> existingAnchors, PlanFrame frame) {
        if (fitsPinWindow(desiredTick, duration, existingAnchors, frame)) {
            return desiredTick;
        }

        int snapped = desiredTick + PIN_SNAP_BUCKET_TICKS;
        if (fitsPinWindow(snapped, duration, existingAnchors, frame)) {
            return snapped;
        }

        return -1;
    }

    /**
     * A candidate pin tick is feasible when it starts within the waking frame and its interval
     * doesn't overlap any anchor already placed.
     */
    private static boolean fitsPinWindow(int tick, int duration, List<PlanAnchor> anchors, PlanFrame frame) {
        if (tick < frame.wakeCivil() || tick > frame.bedtimeCivil()) {
            return false;
        }

        int end = tick + duration;
        for (PlanAnchor anchor : anchors) {
            int anchorEnd = anchor.startTick() + anchor.durationTicks();
            if (tick < anchorEnd && anchor.startTick() < end) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the derived band whose window contains a demoted pin's originally-requested civil
     * tick, so it re-enters that band's ordinary fill preference. {@code null} when the tick falls
     * outside every derived band (e.g. inside a reserved anchor gap) — the pin is then dropped
     * entirely, an acceptable degradation (composition doc §6.5).
     */
    @Nullable
    private static String findBandId(List<PlanBand> bands, int civilTick) {
        for (PlanBand band : bands) {
            if (civilTick >= band.startCivil() && civilTick < band.endCivil()) {
                return band.id();
            }
        }
        return null;
    }

    @Nullable
    private static WeightedBehavior findBehavior(PlanGenerationContext context, BehaviorKey key) {
        return context.availableBehaviors().stream()
                .filter(behavior -> behavior.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Chooses which pool/multiplier/strategy to pack it with for a single band.
     */
    private record BandFill(List<WeightedBehavior> pool,
                            PlannerPolicy.EffectiveWeightMultiplier multiplier,
                            SlotSelectionStrategy strategy) {
    }

    private static BandFill fillPolicyFor(PlanBand band, PlanIntent intent, PlanGenerationContext context,
                                          Map<String, List<BehaviorKey>> demotedSelections) {
        if (intent.selections().isEmpty()) {
            // Pure-heuristic: pack the band's own derived pool with the coverage-first weighted-random strategy
            return new BandFill(band.pool(), band.baseMultiplier(), HEURISTIC_STRATEGY);
        }

        // The intelligent overlay path draws from the FULL available menu, not just this band's derived pool.
        // Demoted pins fall in behind the model's own ordinary preference order rather than displacing it.
        List<BehaviorKey> selectedKeysInOrder = Stream.concat(
                        intent.selections().getOrDefault(band.id(), List.of()).stream(),
                        demotedSelections.getOrDefault(band.id(), List.of()).stream())
                .distinct()
                .toList();
        List<WeightedBehavior> filteredPool = context.availableBehaviors().stream()
                .filter(behavior -> selectedKeysInOrder.contains(behavior.key()))
                .toList();

        // Honor the model's explicit order, so weight is irrelevant to WHICH slot is chosen —
        // but the band's own rest-day base multiplier would still zero-drop an explicitly-selected
        // behavior (e.g. WORK + NONE) via WindowPacker's weight() > 0 filter.
        // Identity keeps every selection packable; the opportunity multiplier (0.2 or 1.0, never 0) is layered on separately below.
        return new BandFill(filteredPool, PlannerPolicy.DEFAULT_EFFECTIVE_WEIGHT_MULTIPLIER,
                new OrderedPreferenceSelectionStrategy(selectedKeysInOrder));
    }

    /**
     * Fills a single band's free sub-intervals (band bounds minus any anchor reservation) using the greedy cooldown-aware packer.
     */
    private void fillBand(List<PlanSlot> slots,
                          PlanBand band,
                          PlanIntent intent,
                          PlanGenerationContext context,
                          AnchorPlacement anchorPlacement,
                          Random random) {
        List<int[]> freeIntervals = freeSubIntervals(band.startCivil(), band.endCivil(), anchorPlacement.anchors());
        if (freeIntervals.isEmpty()) {
            return;
        }

        BandFill fill = fillPolicyFor(band, intent, context, anchorPlacement.demotedSelections());
        if (fill.pool().isEmpty()) {
            return;
        }

        // Fold in the opportunity factor on top of the base policy multiplier
        PlannerPolicy.EffectiveWeightMultiplier multiplier = fill.multiplier().andThen(PlannerPolicy.opportunityMultiplier(context));

        // Build packing candidates — effective weight folds the day-type multiplier in, so rest-day
        // suppression of heavy work is invisible to the packer itself (it just sees lower weight -> 0).
        List<WindowPacker.PackingCandidate> candidates = fill.pool().stream()
                .map(behavior -> new WindowPacker.PackingCandidate(
                        behavior.key(),
                        Math.max(0.0D, behavior.weight() * multiplier.apply(behavior)),
                        behavior.descriptor().getEstimatedDuration().getTicksAsInt(),
                        behavior.descriptor().getCooldown()))
                .toList();

        for (int[] interval : freeIntervals) {
            // A fresh copy of the pin baseline per interval: the pinned eligibility floor is visible
            // to every fill call (cross-pass cadence), but ordinary fill draws from one interval never
            // leak into the next.
            Map<BehaviorKey, Integer> nextEligible = new HashMap<>(anchorPlacement.pinnedEligibility());
            List<WindowPacker.PackedSlot> packed = WindowPacker.pack(candidates, interval[0], interval[1],
                    band.basePriority(), fill.strategy(), random, band.packFactor(), nextEligible);
            for (WindowPacker.PackedSlot packedSlot : packed) {
                slots.add(PlanSlot.builder()
                        .startTick(packedSlot.startCivil())
                        .behaviorKey(packedSlot.key())
                        .priority(packedSlot.priority())
                        .flexible(true)
                        .estimatedDurationTicks(packedSlot.durationTicks())
                        .build());
            }
        }
    }

    /**
     * Splits {@code [bandStart, bandEnd)} into the sub-intervals not covered by any anchor
     * reservation overlapping the band, in ascending order.
     */
    @VisibleForTesting
    static List<int[]> freeSubIntervals(int bandStart, int bandEnd, List<PlanAnchor> anchors) {
        List<int[]> reserved = anchors.stream()
                .map(anchor -> new int[]{anchor.startTick(), anchor.startTick() + anchor.durationTicks()})
                .filter(interval -> interval[1] > bandStart && interval[0] < bandEnd)
                .sorted(Comparator.comparingInt(a -> a[0]))
                .toList();

        List<int[]> free = new ArrayList<>();
        int cursor = bandStart;
        for (int[] reservedInterval : reserved) {
            int resolvedStart = Math.max(reservedInterval[0], bandStart);
            int resolvedEnd = Math.min(reservedInterval[1], bandEnd);
            if (resolvedStart > cursor) {
                free.add(new int[]{cursor, resolvedStart});
            }
            cursor = Math.max(cursor, resolvedEnd);
        }
        if (cursor < bandEnd) {
            free.add(new int[]{cursor, bandEnd});
        }
        return free;
    }

    private static DayPlanSchedule buildActivitySchedule(PlanGenerationContext context, PlanFrame frame) {
        int wakeCivil = frame.wakeCivil();
        int bedtimeCivil = frame.bedtimeCivil();

        DayPlanSchedule.DayPlanScheduleBuilder builder = DayPlanSchedule.builder()
                .wakeTick(wakeCivil)
                .bedtimeTick(bedtimeCivil);

        // Build rest days or nitwit with no work blocks
        if (context.dayType() == PlanDayType.REST_DAY || context.profession().equals(VillagerProfessionKey.NITWIT)) {
            int meetStartCivil = restDayMeetPivotCivil(bedtimeCivil);
            if (meetStartCivil > wakeCivil) {
                builder.activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(wakeCivil)
                        .endTick(meetStartCivil)
                        .build());
            }
            if (bedtimeCivil > meetStartCivil) {
                builder.activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.MEET)
                        .startTick(meetStartCivil)
                        .endTick(bedtimeCivil)
                        .build());
            }
            return builder.build();
        }

        // The WORK block derives straight from the frame's own workStart/workEnd, independent of
        // however deriveBands shapes the MORNING band from the same two numbers — blocks and bands
        // are different bound sets computed from a shared frame
        int workStart = Math.min(frame.workStartCivil(), bedtimeCivil);
        int workEnd = Math.clamp(frame.workEndCivil(), workStart, bedtimeCivil);

        if (workStart > wakeCivil) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.IDLE)
                    .startTick(wakeCivil)
                    .endTick(workStart)
                    .build());
        }
        if (workEnd > workStart) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.WORK)
                    .startTick(workStart)
                    .endTick(workEnd)
                    .build());
        }
        if (bedtimeCivil > workEnd) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.MEET)
                    .startTick(workEnd)
                    .endTick(bedtimeCivil)
                    .build());
        }
        return builder.build();
    }

    /**
     * Computes the villager's work-end tick (Minecraft space — {@link ScheduleProfile} and the
     * {@link TimeOfDay} clamp bounds used here are all authored in MC space; the result is
     * converted to civil once, at the {@link #computeFrame} call site).
     * WIL determines commitment: high WIL works up to 45 game minutes longer, low WIL quits earlier.
     */
    private static int computeWorkEndTick(ScheduleProfile schedule, GeneticsProfile genetics, PlanDayType dayType) {
        int workEndTick = schedule.workEndTick();
        if (dayType == PlanDayType.REST_DAY) {
            workEndTick = Math.min(workEndTick, TimeOfDay.AT_13_00.getMinecraftTick());
        }

        workEndTick += (int) ((genetics.getGeneValue(GeneType.WILL) - 0.5) * GameTicks.minutes(90).getTicksAsInt());
        return Math.clamp(workEndTick, TimeOfDay.AT_10_00.getMinecraftTick(), TimeOfDay.AT_17_00.getMinecraftTick());
    }

    /**
     * Civil-space work-start anchor for the villager's profession.
     * <p>
     * On a rest day the +1h sleep-in — compounded by a positive chronotype offset — can push the
     * villager's wake past its own work-start time. In civil space, that shows up as
     * {@code workStartCivil < wakeCivil} (no wrap needed to detect it). When work-start no longer
     * leads wake — or its position lands at or past bedtime, i.e. outside today's waking window —
     * there is no real pre-work block, so the pre-work gap collapses to the morning floor.
     */
    private static int resolveWorkStartCivil(ScheduleProfile schedule, int wakeCivil, int bedtimeCivil) {
        int morningFloorCivil = wakeCivil + GameTicks.minutes(30).getTicksAsInt();
        int workStartCivil = CivilTime.civilFromMcTick(schedule.workStartTick());
        if (workStartCivil >= bedtimeCivil || workStartCivil < wakeCivil) {
            return morningFloorCivil;
        }
        return Math.max(morningFloorCivil, workStartCivil);
    }

    /**
     * The rest/nitwit IDLE → MEET pivot: a fixed buffer past the lunch anchor, clamped to bedtime.
     */
    private static int restDayMeetPivotCivil(int bedtimeCivil) {
        return Math.min(LUNCH_BAND_BOUND_CIVIL_TICK + BAND_EDGE_MARGIN_TICKS, bedtimeCivil);
    }

}
