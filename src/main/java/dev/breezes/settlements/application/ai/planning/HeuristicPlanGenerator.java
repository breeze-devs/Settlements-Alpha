package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
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
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final int MINIMUM_SLOT_SPACING_TICKS = GameTicks.minutes(20).getTicksAsInt();

    private final IWakeTickResolver wakeTickResolver;

    @Override
    public DayPlan generate(PlanGenerationContext context) {
        // availableBehaviors is pre-filtered by profession by BehaviorPoolResolver; rest-day policy is applied by PlannerPalette
        List<WeightedBehavior> availableBehaviors = context.availableBehaviors();
        ScheduleProfile schedule = context.scheduleProfile();
        RestDayPolicy restDayPolicy = context.restDayPolicy();

        PlannerPalette palette = new PlannerPalette(availableBehaviors);
        List<PlanSlot> slots = new ArrayList<>();

        // All internal range arithmetic uses a linear offset from wake (epoch) to handle early-bird professions
        // whose wake tick is numerically > 0 (e.g., 5 am is at tick 23,000).
        int epoch = this.computeWakeTick(schedule, context.genetics(), context.dayType());

        int workStartLinear = Math.max(GameTicks.minutes(30).getTicksAsInt(), toLinear(schedule.workStartTick(), epoch));
        int workEndLinear = toLinear(this.computeWorkEndTick(schedule, context.genetics(), context.dayType()), epoch);
        int lunchLinear = toLinear(LUNCH_TICK, epoch);
        DayPlanSchedule daySchedule = this.buildActivitySchedule(context, schedule, epoch, workStartLinear, workEndLinear);

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

        slots.add(PlanSlot.builder()
                .startTick(clampTick(LUNCH_TICK))
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(95)
                .flexible(false)
                .estimatedDurationTicks(GameTicks.minutes(10).getTicksAsInt())
                .reason("Shared lunch timing creates a natural village-wide overlap for social availability.")
                .build());
        this.addAfternoonSlots(slots, context, restDayPolicy, palette,
                Math.max(workEndLinear, lunchLinear + 1_000), epoch);
        slots.add(PlanSlot.builder()
                .startTick(clampTick(DINNER_TICK))
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
                                                  ScheduleProfile schedule,
                                                  int wakeTick,
                                                  int workStartLinear,
                                                  int workEndLinear) {
        int bedtimeLinear = toLinear(schedule.defaultSleepTick(), wakeTick);
        DayPlanSchedule.DayPlanScheduleBuilder builder = DayPlanSchedule.builder()
                .wakeTick(wakeTick)
                .bedtimeTick(schedule.defaultSleepTick());

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

        int minEndLinear = workStartLinear + MINIMUM_SLOT_SPACING_TICKS;

        // Ensure the max upper bound (Lunch) is NEVER smaller than the minimum bound.
        // If work starts after lunch, this safely pushes the max bound forward.
        int maxEndLinear = Math.max(minEndLinear, lunchLinear - 500);

        int endLinear = Math.clamp(workEndLinear, minEndLinear, maxEndLinear);

        this.packWindow(slots, workBehaviors, workStartLinear, endLinear, 70, epoch, true,
                behavior -> 1.0D,
                "Profession work block selected by the deterministic heuristic planner.");
    }

    private void addRestDaySlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                 PlannerPalette palette, int epoch) {
        List<WeightedBehavior> restOptions = palette.availableBehaviors();
        // Wake is always linear=0, so 40 minutes after wake is simply the 40-min offset.
        int firstOpenLinear = GameTicks.minutes(40).getTicksAsInt();
        int endLinear = toLinear(LUNCH_TICK, epoch) - 500;

        this.packWindow(slots, restOptions, firstOpenLinear, endLinear, 55, epoch, false,
                behavior -> restDayMultiplier(behavior.descriptor(), policy),
                "Rest days favor low-intensity, social, and leisure behaviors.");
    }

    private void addAfternoonSlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                   PlannerPalette palette, int earliestLinear, int epoch) {
        int afternoonStartLinear = Math.max(earliestLinear, toLinear(TimeOfDay.AT_13_00.getTick(), epoch));
        int afternoonEndLinear = Math.min(
                toLinear(DINNER_TICK, epoch) - 500,
                toLinear(TimeOfDay.AT_16_30.getTick(), epoch));
        List<WeightedBehavior> candidates = palette.afternoonCandidates(context.genetics());

        EffectiveWeightMultiplier multiplier = context.dayType() == PlanDayType.REST_DAY
                ? behavior -> restDayMultiplier(behavior.descriptor(), policy)
                : behavior -> 1.0D;
        this.packWindow(slots, candidates, afternoonStartLinear, afternoonEndLinear, 50, epoch, true, multiplier,
                "Afternoon slots mix remaining duties with decompression and social time.");
    }

    /**
     * Fills a linear window with repeated behavior slots according to effective weights.
     * <p>
     * Weight math stays floating-point until Hamilton allocation so rest-day multipliers can reduce
     * heavy work without deleting light work through premature rounding.
     */
    private void packWindow(List<PlanSlot> slots, List<WeightedBehavior> pool, int startLinear, int endLinear,
                            int priority, int epoch, boolean enforceMinOne,
                            EffectiveWeightMultiplier multiplier, String reason) {
        if (pool.isEmpty() || endLinear <= startLinear) {
            return;
        }

        List<AllocationCandidate> candidates = pool.stream()
                .map(behavior -> new AllocationCandidate(behavior, Math.max(0.0D, behavior.weight() * multiplier.apply(behavior))))
                .filter(candidate -> candidate.effectiveWeight() > 0.0D)
                .toList();
        double totalEffectiveWeight = candidates.stream()
                .mapToDouble(AllocationCandidate::effectiveWeight)
                .sum();
        if (totalEffectiveWeight <= 0.0D) {
            log.behaviorWarn("packWindow skipped: zero total effective weight");
            return;
        }

        int windowLength = endLinear - startLinear;
        double weightedAverageDuration = candidates.stream()
                .mapToDouble(candidate -> durationTicks(candidate.behavior()) * candidate.effectiveWeight())
                .sum() / totalEffectiveWeight;
        int capacity = Math.max(1, (int) (windowLength / Math.max(1.0D, weightedAverageDuration)));

        List<AllocatedBehavior> allocation = this.allocateHamilton(candidates, totalEffectiveWeight, capacity, enforceMinOne);
        ArrayList<WeightedBehavior> packed = new ArrayList<>();
        for (AllocatedBehavior allocated : allocation) {
            for (int count = 0; count < allocated.count(); count++) {
                packed.add(allocated.behavior());
            }
        }
        RandomUtil.shuffle(packed);

        int cursor = startLinear;
        int emitted = 0;
        for (WeightedBehavior behavior : packed) {
            int duration = durationTicks(behavior);
            if (cursor + duration > endLinear) {
                continue;
            }
            slots.add(PlanSlot.builder()
                    .startTick(clampTick(fromLinear(cursor, epoch)))
                    .behaviorKey(behavior.key())
                    .priority(Math.max(0, priority - emitted))
                    .flexible(true)
                    .estimatedDurationTicks(duration)
                    .reason(reason)
                    .build());
            emitted++;
            cursor += Math.max(duration, MINIMUM_SLOT_SPACING_TICKS);
            if (cursor >= endLinear) {
                return;
            }
        }
    }

    private List<AllocatedBehavior> allocateHamilton(List<AllocationCandidate> candidates, double totalEffectiveWeight,
                                                     int capacity, boolean enforceMinOne) {
        List<MutableAllocation> allocations = new ArrayList<>();
        int allocatedCount = 0;
        for (AllocationCandidate candidate : candidates) {
            double rawShare = (candidate.effectiveWeight() / totalEffectiveWeight) * capacity;
            int floorShare = (int) Math.floor(rawShare);
            int count = enforceMinOne && candidate.behavior().weight() > 0 ? Math.max(1, floorShare) : floorShare;
            allocations.add(new MutableAllocation(candidate.behavior(), count, rawShare - floorShare));
            allocatedCount += count;
        }

        if (allocatedCount > capacity) {
            // When many positive entries compete for a tiny window, trim smallest shares first so capacity remains meaningful.
            allocations.sort(Comparator.<MutableAllocation>comparingDouble(MutableAllocation::remainder)
                    .thenComparing(allocation -> allocation.behavior().key().id()));
            int overage = allocatedCount - capacity;
            for (MutableAllocation allocation : allocations) {
                if (overage <= 0) {
                    break;
                }
                if (allocation.count() > 0) {
                    allocation.decrement();
                    overage--;
                }
            }
        } else if (allocatedCount < capacity) {
            allocations.sort(Comparator.<MutableAllocation>comparingDouble(MutableAllocation::remainder).reversed()
                    .thenComparing(allocation -> allocation.behavior().key().id()));
            int remaining = capacity - allocatedCount;
            for (int index = 0; index < remaining; index++) {
                allocations.get(index % allocations.size()).increment();
            }
        }

        return allocations.stream()
                .filter(allocation -> allocation.count() > 0)
                .map(allocation -> new AllocatedBehavior(allocation.behavior(), allocation.count()))
                .toList();
    }

    /**
     * Computes the villager's wake tick for the day.
     * CON determines adherence: high CON wakes on time, low CON sleeps in up to ±30 game minutes.
     * Rest days add a 1-hour (game time) sleep-in regardless of CON.
     * <p>
     * The result may be in the pre-dawn range (ticks > 18,000, i.e. before 6am) for early-bird
     * professions. {@link DayPlan} handles cross-boundary ordering via its {@code dayStartTick} epoch.
     */
    private int computeWakeTick(ScheduleProfile schedule, GeneticsProfile genetics, PlanDayType dayType) {
        return this.wakeTickResolver.resolveWakeTick(schedule, genetics, dayType);
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

    private record AllocationCandidate(WeightedBehavior behavior, double effectiveWeight) {
    }

    private record AllocatedBehavior(WeightedBehavior behavior, int count) {
    }

    private static final class MutableAllocation {

        private final WeightedBehavior behavior;
        private int count;
        private final double remainder;

        private MutableAllocation(WeightedBehavior behavior, int count, double remainder) {
            this.behavior = behavior;
            this.count = count;
            this.remainder = remainder;
        }

        private WeightedBehavior behavior() {
            return this.behavior;
        }

        private int count() {
            return this.count;
        }

        private double remainder() {
            return this.remainder;
        }

        private void increment() {
            this.count++;
        }

        private void decrement() {
            this.count--;
        }

    }

}
