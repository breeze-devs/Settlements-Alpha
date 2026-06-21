package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.Chronotype;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces a villager's daily plan using deterministic heuristics
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class HeuristicPlanGenerator implements IPlanGenerator {

    // Anchors are real game-tick positions (0 = 06:00 AM) used for creating slots with absolute times.
    private static final int LUNCH_TICK = TimeOfDay.AT_12_00.getTick();
    private static final int DINNER_TICK = TimeOfDay.AT_17_30.getTick();

    @Override
    public DayPlan generate(PlanGenerationContext context) {
        // availableBehaviors is pre-filtered by profession by BehaviorPoolResolver; rest-day policy is applied by PlannerPalette
        List<WeightedBehavior> availableBehaviors = context.availableBehaviors();
        ScheduleProfile schedule = context.scheduleProfile();
        RestDayPolicy restDayPolicy = context.restDayPolicy();

        PlannerPalette palette = new PlannerPalette(availableBehaviors);
        List<PlanSlot> slots = new ArrayList<>();

        // Derive epoch from the wake tick already computed by PlanRunner so this generator and the
        // runner always agree — avoids a second (potentially divergent) wake computation.
        int epoch = Math.floorMod(context.wakeAtAbsoluteTick(), TimeOfDay.TICKS_PER_DAY);

        // Compute the chronotype once and reuse within this generation call.
        Chronotype chronotype = Chronotype.of(context.chronotypeSeed());

        // Sleep shifts with the same chronotype offset as wake so early birds sleep early and night
        // owls sleep late — the two are correlated, which is the "early bird / night owl" personality.
        int sleepTick = clampTick(schedule.defaultSleepTick() + chronotype.wakeSleepOffsetTicks());
        int bedtimeLinear = toLinear(sleepTick, epoch);

        int workStartLinear = resolveWorkStartLinear(schedule, epoch, bedtimeLinear);
        int workEndLinear = toLinear(this.computeWorkEndTick(schedule, context.genetics(), context.dayType()), epoch);
        int lunchLinear = toLinear(LUNCH_TICK, epoch);
        DayPlanSchedule daySchedule = this.buildActivitySchedule(context, epoch, sleepTick, bedtimeLinear, workStartLinear, workEndLinear);

        slots.add(PlanSlot.builder()
                .startTick(clampTick(epoch))
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(100)
                .flexible(false)
                .estimatedDurationTicks(GameTicks.minutes(8).getTicksAsInt())
                .reason("Start the day with a plentiful breakfast")
                .build());

        if (context.dayType() == PlanDayType.REST_DAY || context.profession().equals(VillagerProfessionKey.NITWIT)) {
            this.addRestDaySlots(slots, context, restDayPolicy, palette, epoch);
        } else {
            this.addWorkDaySlots(slots, context, palette, workStartLinear, workEndLinear, lunchLinear, epoch);
        }

        // Inject a morning scout slot when the villager has unverified hearsay tips
        if (context.pendingInvestigateTipCount() > 0) {
            addInvestigateScoutSlot(slots, epoch, workStartLinear);
        }

        // Meal jitter shifts the lunch and dinner anchor per villager. The shift is small so the
        // village still has a rough shared meal window (social availability); only the exact lockstep
        // is broken. Breakfast already anchors to epoch and shifts with wake automatically.
        int mealOffset = chronotype.mealOffsetTicks();
        slots.add(PlanSlot.builder()
                .startTick(clampTick(LUNCH_TICK + mealOffset))
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(95)
                .flexible(false)
                .estimatedDurationTicks(GameTicks.minutes(10).getTicksAsInt())
                .reason("Shared lunch timing creates a natural village-wide overlap for social availability.")
                .build());
        this.addAfternoonSlots(slots, context, restDayPolicy, palette,
                Math.max(workEndLinear, lunchLinear + 1_000), epoch);
        slots.add(PlanSlot.builder()
                .startTick(clampTick(DINNER_TICK + mealOffset))
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(90)
                .flexible(false)
                .estimatedDurationTicks(GameTicks.minutes(10).getTicksAsInt())
                .reason("Dinner anchors the evening routine before villagers wind down.")
                .build());

        return DayPlan.builder()
                .slots(slots)
                .dayType(context.dayType())
                .wakeAtAbsoluteTick(context.wakeAtAbsoluteTick())
                .schedule(daySchedule)
                .dayStartTick(epoch)
                .build();
    }

    private DayPlanSchedule buildActivitySchedule(PlanGenerationContext context,
                                                  int wakeTick,
                                                  int sleepTick,
                                                  int bedtimeLinear,
                                                  int workStartLinear,
                                                  int workEndLinear) {
        DayPlanSchedule.DayPlanScheduleBuilder builder = DayPlanSchedule.builder()
                .wakeTick(wakeTick)
                .bedtimeTick(sleepTick);

        // Build rest days or nitwit with no work blocks
        if (context.dayType() == PlanDayType.REST_DAY || context.profession().equals(VillagerProfessionKey.NITWIT)) {
            int meetStartLinear = Math.min(toLinear(TimeOfDay.AT_13_00.getTick(), wakeTick), bedtimeLinear);
            if (meetStartLinear > 0) {
                builder.activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(fromLinear(0, wakeTick))
                        .endTick(fromLinear(meetStartLinear, wakeTick))
                        .reason("Unstructured rest-day time keeps the villager free for low-pressure routines.")
                        .build());
            }
            if (bedtimeLinear > meetStartLinear) {
                builder.activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.MEET)
                        .startTick(fromLinear(meetStartLinear, wakeTick))
                        .endTick(fromLinear(bedtimeLinear, wakeTick))
                        .reason("Afternoon social context gives non-work days visible village life.")
                        .build());
            }
            return builder.build();
        }

        // Build normal days with work blocks
        int workStart = Math.min(workStartLinear, bedtimeLinear);
        int workEnd = Math.clamp(workEndLinear, workStart, bedtimeLinear);

        if (workStart > 0) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.IDLE)
                    .startTick(fromLinear(0, wakeTick))
                    .endTick(fromLinear(workStart, wakeTick))
                    .reason("Morning idle context bridges wake-up routines into the authored work block.")
                    .build());
        }
        if (workEnd > workStart) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.WORK)
                    .startTick(fromLinear(workStart, wakeTick))
                    .endTick(fromLinear(workEnd, wakeTick))
                    .reason("Profession hours define the ambient work context while foreground slots execute selectively.")
                    .build());
        }
        if (bedtimeLinear > workEnd) {
            builder.activityBlock(DayPlanActivityBlock.builder()
                    .context(DayPlanActivityContext.MEET)
                    .startTick(fromLinear(workEnd, wakeTick))
                    .endTick(fromLinear(bedtimeLinear, wakeTick))
                    .reason("Post-work meeting context encourages social ambient life before final bedtime.")
                    .build());
        }
        return builder.build();
    }

    private void addWorkDaySlots(List<PlanSlot> slots, PlanGenerationContext context, PlannerPalette palette,
                                 int workStartLinear, int workEndLinear, int lunchLinear, int epoch) {
        List<WeightedBehavior> workBehaviors = palette.workBehaviors();
        if (workBehaviors.isEmpty()) {
            return;
        }

        int minEndLinear = workStartLinear + WindowPacker.MINIMUM_SLOT_SPACING_TICKS;

        // Ensure the max upper bound (Lunch) is NEVER smaller than the minimum bound.
        // If work starts after lunch, this safely pushes the max bound forward.
        int maxEndLinear = Math.max(minEndLinear, lunchLinear - 500);

        int endLinear = Math.clamp(workEndLinear, minEndLinear, maxEndLinear);

        this.packWindow(slots, workBehaviors, workStartLinear, endLinear, 70, epoch,
                behavior -> 1.0D,
                "Profession work block selected by the deterministic heuristic planner.");
    }

    private void addRestDaySlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                 PlannerPalette palette, int epoch) {
        List<WeightedBehavior> restOptions = palette.availableBehaviors();
        // Wake is always linear=0, so 40 minutes after wake is simply the 40-min offset.
        int firstOpenLinear = GameTicks.minutes(40).getTicksAsInt();
        int endLinear = toLinear(LUNCH_TICK, epoch) - 500;

        this.packWindow(slots, restOptions, firstOpenLinear, endLinear, 55, epoch,
                behavior -> restDayMultiplier(behavior.descriptor(), policy),
                "Rest days favor low-intensity, social, and leisure behaviors.");
    }

    private void addAfternoonSlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                   PlannerPalette palette, int earliestLinear, int epoch) {
        int afternoonStartLinear = Math.max(earliestLinear, toLinear(TimeOfDay.AT_13_00.getTick(), epoch));
        int afternoonEndLinear = Math.min(toLinear(DINNER_TICK, epoch) - 500, toLinear(TimeOfDay.AT_16_30.getTick(), epoch));

        // TODO: A SOCIAL weight multiplier seeded from CHA is the planned follow-up (context-aware weight extension).
        List<WeightedBehavior> candidates = palette.afternoonCandidates(context.genetics());

        EffectiveWeightMultiplier multiplier = context.dayType() == PlanDayType.REST_DAY
                ? behavior -> restDayMultiplier(behavior.descriptor(), policy)
                : behavior -> 1.0D;
        this.packWindow(slots, candidates, afternoonStartLinear, afternoonEndLinear, 50, epoch, multiplier,
                "Afternoon slots mix remaining duties with decompression and social time.");
    }

    /**
     * Injects a single Investigate scout slot
     * <p>
     * The slot fires just after work starts so the villager can verify a tip and return
     * to normal work with fresh knowledge before the main work block runs. The slot is
     * flexible: if the behavior's precondition fails (e.g. no tip configured yet) the
     * slot is skipped cleanly and the rest of the plan is unaffected.
     * <p>
     * Only one slot is injected per plan regardless of how many pending tips exist —
     * re-planning the next morning handles subsequent tips, preventing a single gossip
     * session from dominating an entire day's schedule.
     */
    private static void addInvestigateScoutSlot(List<PlanSlot> slots, int epoch, int workStartLinear) {
        // Place the scout 20 minutes into the work block so breakfast and any early fixed
        // slots have already resolved; avoids a time-0 collision with the breakfast anchor.
        int scoutLinear = workStartLinear + GameTicks.minutes(20).getTicksAsInt();
        slots.add(PlanSlot.builder()
                .startTick(clampTick(fromLinear(scoutLinear, epoch)))
                .behaviorKey(BehaviorKey.INVESTIGATE)
                .priority(75)
                .flexible(true)
                .estimatedDurationTicks(GameTicks.minutes(2).getTicksAsInt())
                .reason("Unverified hearsay tip warrants an early morning scout to confirm or refute the claim.")
                .build());
    }

    /**
     * Fills a linear window with behavior slots using the greedy cooldown-aware packer.
     * <p>
     * Delegates to {@link WindowPacker#pack} which enforces per-key cadence spacing, prefers
     * never-emitted keys (coverage-first), and leaves gaps when the pool is exhausted — so
     * single-behavior pools like Nitwit produce sparsity rather than repeats.
     */
    private void packWindow(List<PlanSlot> slots, List<WeightedBehavior> pool, int startLinear, int endLinear,
                            int priority, int epoch, EffectiveWeightMultiplier multiplier, String reason) {
        if (pool.isEmpty() || endLinear <= startLinear) {
            return;
        }

        // Build packing candidates — effective weight folds the day-type multiplier in, so rest-day
        // suppression of heavy work is invisible to the packer itself (it just sees lower weight → 0).
        List<WindowPacker.PackingCandidate> candidates = pool.stream()
                .map(behavior -> new WindowPacker.PackingCandidate(
                        behavior.key(),
                        Math.max(0.0D, behavior.weight() * multiplier.apply(behavior)),
                        durationTicks(behavior),
                        behavior.descriptor().getCooldown()))
                .toList();

        List<WindowPacker.PackedSlot> packed = WindowPacker.pack(candidates, startLinear, endLinear, priority);

        for (WindowPacker.PackedSlot packedSlot : packed) {
            slots.add(PlanSlot.builder()
                    .startTick(clampTick(fromLinear(packedSlot.startLinear(), epoch)))
                    .behaviorKey(packedSlot.key())
                    .priority(packedSlot.priority())
                    .flexible(true)
                    .estimatedDurationTicks(packedSlot.durationTicks())
                    .reason(reason)
                    .build());
        }
    }

    /**
     * Computes the villager's work-end tick.
     * WIL determines commitment: high WIL works up to 45 game minutes longer, low WIL quits earlier.
     */
    private int computeWorkEndTick(ScheduleProfile schedule, GeneticsProfile genetics, PlanDayType dayType) {
        int workEndTick = schedule.workEndTick();
        if (dayType == PlanDayType.REST_DAY) {
            workEndTick = Math.min(workEndTick, TimeOfDay.AT_13_00.getTick());
        }
        workEndTick += (int) ((genetics.getGeneValue(GeneType.WILL) - 0.5) * GameTicks.minutes(90).getTicksAsInt());
        return clampTick(Math.clamp(workEndTick, TimeOfDay.AT_10_00.getTick(), TimeOfDay.AT_17_00.getTick()));
    }

    /**
     * Linear offset from wake to the profession's authored work-start.
     * <p>
     * On a rest day the +1h sleep-in — compounded by a positive chronotype offset — can push the
     * villager's wake past its own work-start time. {@link #toLinear} would then wrap that
     * already-elapsed anchor to nearly a full day, flinging any work-relative slot (notably the
     * morning Investigate scout) across the day boundary and tripping {@code DayPlan}'s slot-window
     * validation. When work-start no longer leads wake — its forward offset lands at or past bedtime,
     * i.e. outside today's waking window — there is no real pre-work block, so the pre-work gap
     * collapses to the morning floor.
     */
    private static int resolveWorkStartLinear(ScheduleProfile schedule, int epoch, int bedtimeLinear) {
        int morningFloorLinear = GameTicks.minutes(30).getTicksAsInt();
        int workStartLinear = toLinear(schedule.workStartTick(), epoch);
        if (workStartLinear >= bedtimeLinear) {
            return morningFloorLinear;
        }
        return Math.max(morningFloorLinear, workStartLinear);
    }

    /**
     * Converts a real game tick to a linear offset from {@code epoch}, wrapping correctly across
     * the 24 000-tick day boundary. Used so all range arithmetic in the generator stays monotonic.
     */
    private static int toLinear(int tick, int epoch) {
        return Math.floorMod(tick - epoch, TimeOfDay.TICKS_PER_DAY);
    }

    /**
     * Converts a linear offset back to a real game tick given the plan's {@code epoch}.
     */
    private static int fromLinear(int linear, int epoch) {
        return (epoch + linear) % TimeOfDay.TICKS_PER_DAY;
    }

    private static int clampTick(int tick) {
        return Math.floorMod(tick, TimeOfDay.TICKS_PER_DAY);
    }

    private static int durationTicks(WeightedBehavior behavior) {
        return Math.max(1, behavior.descriptor().getEstimatedDuration().getTicksAsInt());
    }

    private static double restDayMultiplier(BehaviorPlanningMetadata behavior, RestDayPolicy policy) {
        if (behavior.getCategory() == BehaviorCategory.SOCIAL) {
            return policy.socialMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.SELF_CARE) {
            return policy.selfCareMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.LEISURE) {
            return policy.leisureMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.WORK && behavior.getIntensity() == WorkIntensity.LIGHT) {
            return policy.lightWorkMultiplier();
        }
        if (behavior.getCategory() == BehaviorCategory.WORK && behavior.getIntensity() == WorkIntensity.HEAVY) {
            return policy.heavyWorkMultiplier();
        }
        return 0.0D;
    }

    /**
     * Encapsulates behavior filtering and key resolution for a single plan generation call.
     * All methods are pure queries over the provided behavior list — no state is mutated.
     * <p>
     * The list is pre-filtered for profession by {@code BehaviorPoolResolver}.
     * Rest-day policy weighting is applied inside {@link #packWindow} so fractional multipliers
     * are preserved until allocation.
     */
    private record PlannerPalette(List<WeightedBehavior> availableBehaviors) {

        List<WeightedBehavior> workBehaviors() {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.descriptor().getCategory() == BehaviorCategory.WORK)
                    .toList();
        }

        /**
         * Afternoon candidates ordered by social preference: social-first for high-CHA villagers,
         * social-last for low-CHA — then light work, then leisure fills the remainder.
         * <p>
         * NOTE: under the new weighted-random pick in {@link WindowPacker}, the list ORDER no longer
         * affects which key is chosen — only weight does. The CHA-ordering here is preserved for
         * readability/future use; a proper SOCIAL weight multiplier from CHA is the planned fix.
         */
        List<WeightedBehavior> afternoonCandidates(GeneticsProfile genetics) {
            boolean socialPreference = genetics.getGeneValue(GeneType.CHARISMA) >= 0.45;
            List<WeightedBehavior> social = this.byCategory(BehaviorCategory.SOCIAL);
            List<WeightedBehavior> selfCare = this.byCategory(BehaviorCategory.SELF_CARE);
            List<WeightedBehavior> leisure = this.byCategory(BehaviorCategory.LEISURE);
            List<WeightedBehavior> lightWork = this.workBehaviors().stream()
                    .filter(behavior -> behavior.descriptor().getIntensity() == WorkIntensity.LIGHT)
                    .toList();

            List<WeightedBehavior> candidates = new ArrayList<>();
            if (socialPreference) {
                candidates.addAll(social);
            }

            candidates.addAll(lightWork);
            candidates.addAll(selfCare);
            candidates.addAll(leisure);

            if (!socialPreference) {
                candidates.addAll(social);
            }

            return candidates;
        }

        private List<WeightedBehavior> byCategory(BehaviorCategory category) {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.descriptor().getCategory() == category)
                    .toList();
        }

    }

    @FunctionalInterface
    private interface EffectiveWeightMultiplier {
        double apply(WeightedBehavior behavior);
    }

}
