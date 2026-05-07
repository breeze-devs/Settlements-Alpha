package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Produces a villager's daily plan using deterministic heuristics
 */
public class HeuristicPlanGenerator implements IPlanGenerator {

    // Anchors are real game-tick positions (0 = 06:00 AM) used for creating slots with absolute times.
    private static final int LUNCH_TICK = TimeOfDay.AT_12_00.getTick();
    private static final int DINNER_TICK = TimeOfDay.AT_17_30.getTick();

    private static final int MINIMUM_SLOT_SPACING_TICKS = GameTicks.minutes(20).getTicksAsInt();

    @Inject
    public HeuristicPlanGenerator() {
    }

    @Override
    public DayPlan generate(PlanGenerationContext context) {
        // availableBehaviors is pre-filtered by profession by BehaviorPoolResolver; rest-day policy is applied by PlannerPalette
        List<BehaviorPlanningMetadata> availableBehaviors = context.availableBehaviors();
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
                .generatedForDay(context.gameDay())
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
        List<BehaviorPlanningMetadata> workBehaviors = palette.workBehaviors();
        if (workBehaviors.isEmpty()) {
            return;
        }

        int targetCount = this.computeWorkSlotCount(context.genetics(), workBehaviors.size());

        int minEndLinear = workStartLinear + MINIMUM_SLOT_SPACING_TICKS;

        // Ensure the max upper bound (Lunch) is NEVER smaller than the minimum bound.
        // If work starts after lunch, this safely pushes the max bound forward.
        int maxEndLinear = Math.max(minEndLinear, lunchLinear - 500);

        int endLinear = Math.clamp(workEndLinear, minEndLinear, maxEndLinear);

        List<BehaviorPlanningMetadata> ordered = this.rotateByProfession(workBehaviors, context.profession(), context.gameDay());
        this.addDistributedBehaviorSlots(slots, ordered, targetCount, workStartLinear, endLinear, 70, epoch,
                "Profession work block selected by the deterministic heuristic planner.");
    }

    private void addRestDaySlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                 PlannerPalette palette, int epoch) {
        List<BehaviorPlanningMetadata> restOptions = palette.restDayCandidates(policy);
        int targetCount = 2 + this.socialBonus(context.genetics());
        // Wake is always linear=0, so 40 minutes after wake is simply the 40-min offset.
        int firstOpenLinear = GameTicks.minutes(40).getTicksAsInt();
        int endLinear = toLinear(LUNCH_TICK, epoch) - 500;

        this.addDistributedBehaviorSlots(slots, restOptions,
                targetCount, firstOpenLinear, endLinear, 55, epoch,
                "Rest days favor low-intensity, social, and leisure behaviors.");
    }

    private void addAfternoonSlots(List<PlanSlot> slots, PlanGenerationContext context, RestDayPolicy policy,
                                   PlannerPalette palette, int earliestLinear, int epoch) {
        int afternoonStartLinear = Math.max(earliestLinear, toLinear(TimeOfDay.AT_13_00.getTick(), epoch));
        int afternoonEndLinear = Math.min(
                toLinear(DINNER_TICK, epoch) - 500,
                toLinear(TimeOfDay.AT_16_30.getTick(), epoch));
        List<BehaviorPlanningMetadata> candidates = context.dayType() == PlanDayType.REST_DAY
                ? palette.restDayCandidates(policy)
                : palette.afternoonCandidates(context.genetics());

        int targetCount = context.dayType() == PlanDayType.REST_DAY ? 3 : 2 + this.socialBonus(context.genetics());
        // Use gameDay+1 as the rotation seed so afternoon ordering differs from the morning block on the same day.
        List<BehaviorPlanningMetadata> orderedCandidates = context.dayType() == PlanDayType.REST_DAY
                ? candidates
                : this.rotateByProfession(candidates, context.profession(), context.gameDay() + 1);
        this.addDistributedBehaviorSlots(slots, orderedCandidates,
                targetCount, afternoonStartLinear, afternoonEndLinear, 50, epoch,
                "Afternoon slots mix remaining duties with decompression and social time.");
    }

    /**
     * Distributes {@code targetCount} slots evenly across the linear range {@code [startLinear, endLinear]}.
     * All range arithmetic is performed in linear (epoch-relative) space; real game ticks are recovered
     * via {@link #fromLinear} before slot creation. Behaviors are taken in order with wrap-around.
     */
    private void addDistributedBehaviorSlots(List<PlanSlot> slots, List<BehaviorPlanningMetadata> behaviors,
                                             int targetCount, int startLinear, int endLinear, int priority,
                                             int epoch, String reason) {
        if (behaviors.isEmpty() || targetCount <= 0 || endLinear < startLinear) {
            return;
        }

        // Cannot schedule more slots than requested, nor more than are available for variety.
        int count = Math.min(targetCount, behaviors.size());
        int spacing = Math.max(MINIMUM_SLOT_SPACING_TICKS, (endLinear - startLinear) / count);
        for (int index = 0; index < count; index++) {
            BehaviorPlanningMetadata behavior = behaviors.get(index % behaviors.size());
            int linearStart = Math.min(endLinear, startLinear + (index * spacing));
            slots.add(PlanSlot.builder()
                    .startTick(clampTick(fromLinear(linearStart, epoch)))
                    .behaviorKey(behavior.getKey())
                    .priority(Math.max(0, priority - index))
                    .flexible(true)
                    .estimatedDurationTicks(Math.max(1, behavior.getEstimatedDuration().getTicksAsInt()))
                    .reason(reason)
                    .build());
        }
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
        int wakeTick = schedule.defaultWakeTick();
        if (dayType == PlanDayType.REST_DAY) {
            wakeTick += GameTicks.hours(1).getTicksAsInt();
        }
        wakeTick += (int) ((0.5 - genetics.getGeneValue(GeneType.CONSTITUTION)) * GameTicks.minutes(60).getTicksAsInt());
        return clampTick(wakeTick);
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
     * How many work slots to fill in the morning block.
     * High WIL and AGI each add one extra slot, capped by how many distinct work behaviors are available.
     */
    private int computeWorkSlotCount(GeneticsProfile genetics, int availableWorkCount) {
        int baseCount = 2;
        if (genetics.getGeneValue(GeneType.WILL) > 0.7) {
            baseCount++;
        }
        if (genetics.getGeneValue(GeneType.AGILITY) > 0.7) {
            baseCount++;
        }

        // TODO: [agent] this is not necessarily true, also we should add work-idle behavior
        // Clamp to [1, baseCount]: cannot exceed available behaviors (variety), must schedule at least one.
        return Math.clamp(availableWorkCount, 1, baseCount);
    }

    /**
     * Returns 1–2 extra social slots for high-CHA villagers, 0 for low-CHA.
     * Used to increase gossip / trade presence on both work and rest afternoons.
     */
    private int socialBonus(GeneticsProfile genetics) {
        double charisma = genetics.getGeneValue(GeneType.CHARISMA);
        if (charisma > 0.75) {
            return 2;
        }
        if (charisma > 0.45) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns {@code behaviors} in a deterministic but day-varying order.
     * <p>
     * Using profession hash + game day as a rotation offset means:
     * <ul>
     *   <li>The same villager sees the same order every time the plan is regenerated for a given day
     *       (reproducible, no RNG drift).</li>
     *   <li>Different professions or different days start from a different behavior, avoiding the
     *       monotony of always executing behaviors in the same alphabetical sequence.</li>
     * </ul>
     * Sorting by key id before rotating ensures the rotation is stable across catalog changes
     * that don't alter keys.
     */
    private List<BehaviorPlanningMetadata> rotateByProfession(List<BehaviorPlanningMetadata> behaviors,
                                                              VillagerProfessionKey profession, long gameDay) {
        if (behaviors.size() <= 1) {
            return behaviors;
        }

        List<BehaviorPlanningMetadata> sorted = behaviors.stream()
                .sorted(Comparator.comparing(b -> b.getKey().id()))
                .toList();
        int offset = Math.floorMod(profession.id().hashCode() + Long.hashCode(gameDay), sorted.size());
        List<BehaviorPlanningMetadata> rotated = new ArrayList<>(sorted.size());
        rotated.addAll(sorted.subList(offset, sorted.size()));
        rotated.addAll(sorted.subList(0, offset));
        return rotated;
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

    /**
     * Encapsulates behavior filtering and key resolution for a single plan generation call.
     * All methods are pure queries over the provided behavior list — no state is mutated.
     * <p>
     * The list is pre-filtered for profession by {@code BehaviorPoolResolver}.
     * Rest-day policy filtering is applied here via {@link #restDayCandidates}.
     */
    private record PlannerPalette(List<BehaviorPlanningMetadata> availableBehaviors) {

        List<BehaviorPlanningMetadata> workBehaviors() {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.getCategory() == BehaviorCategory.WORK)
                    .toList();
        }

        /**
         * Afternoon candidates ordered by social preference: social-first for high-CHA villagers,
         * social-last for low-CHA — then light work, then leisure fills the remainder.
         */
        List<BehaviorPlanningMetadata> afternoonCandidates(GeneticsProfile genetics) {
            boolean socialPreference = genetics.getGeneValue(GeneType.CHARISMA) >= 0.45;
            List<BehaviorPlanningMetadata> social = this.byCategory(BehaviorCategory.SOCIAL);
            List<BehaviorPlanningMetadata> leisure = this.byCategory(BehaviorCategory.LEISURE);
            List<BehaviorPlanningMetadata> lightWork = this.workBehaviors().stream()
                    .filter(behavior -> behavior.getIntensity() == WorkIntensity.LIGHT)
                    .toList();

            List<BehaviorPlanningMetadata> candidates = new ArrayList<>();
            if (socialPreference) {
                candidates.addAll(social);
            }
            candidates.addAll(lightWork);
            candidates.addAll(leisure);
            if (!socialPreference) {
                candidates.addAll(social);
            }
            return candidates;
        }

        /**
         * Rest-day candidates ranked by policy weight, computed once to avoid O(n log n) recomputation
         * during sorting. Behaviors with zero (or negative) weight are excluded.
         */
        List<BehaviorPlanningMetadata> restDayCandidates(RestDayPolicy policy) {
            record Weighted(BehaviorPlanningMetadata behavior, float weight) {
            }
            return this.availableBehaviors.stream()
                    .map(behavior -> new Weighted(behavior, this.restDayWeight(behavior, policy)))
                    .filter(w -> w.weight() > 0.0F)
                    .sorted(Comparator.<Weighted, Float>comparing(Weighted::weight, Comparator.reverseOrder())
                            .thenComparing(w -> w.behavior().getKey().id()))
                    .map(Weighted::behavior)
                    .toList();
        }

        private List<BehaviorPlanningMetadata> byCategory(BehaviorCategory category) {
            return this.availableBehaviors.stream()
                    .filter(behavior -> behavior.getCategory() == category)
                    .toList();
        }

        private float restDayWeight(BehaviorPlanningMetadata behavior, RestDayPolicy policy) {
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
            return 0.0F;
        }

    }

}
