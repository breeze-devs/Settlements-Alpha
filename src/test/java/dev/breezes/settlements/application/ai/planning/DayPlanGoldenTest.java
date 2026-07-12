package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-master lock for the plan-path RNG contract introduced in slice P1b: plan generation
 * draws every random choice (start-phase jitter, gap advance, cooldown spacing, weighted
 * selection) from a single {@link java.util.Random} seeded by {@link PlanGenerationContext#planSeed()},
 * so the exact same (profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed) tuple must
 * always reproduce the exact same plan.
 * <p>
 * The literal tuples pinned below lock the draw sequence and tick values. The P1c civil-time sweep
 * (tick 0 = midnight rather than 06:00 dawn) re-based every literal via
 * {@code civil = floorMod(mcTick + 6000, 24000)} — ONLY each slot's {@code startTick} and each
 * schedule bound moved; the behavior key, priority, duration, flexibility, and RELATIVE ordering of
 * every slot are unchanged from the pre-sweep baseline. Any other diff going forward means
 * something broke behavior, not just re-based ticks.
 * <p>
 * Fixtures are hand-built (no Minecraft objects), mirroring {@link HeuristicPlanGeneratorTest}'s style.
 */
class DayPlanGoldenTest {

    private final HeuristicPlanGenerator generator = new HeuristicPlanGenerator(new DayPlanComposer(DefaultMealAnchorTable.rows()));

    // Test-local behavior keys — deliberately NOT real catalog keys, so this golden lock is
    // insulated from unrelated catalog changes and only exercises the packing/RNG contract.
    private static final BehaviorKey WORK_HEAVY = BehaviorKey.of("golden_work_heavy");
    private static final BehaviorKey WORK_LIGHT = BehaviorKey.of("golden_work_light");
    private static final BehaviorKey SOCIAL = BehaviorKey.of("golden_social");
    private static final BehaviorKey SELF_CARE = BehaviorKey.of("golden_self_care");
    private static final BehaviorKey LEISURE = BehaviorKey.of("golden_leisure");

    // Non-degenerate cooldown ranges so CooldownRange#drawTicks actually draws (a min==max range
    // would short-circuit without touching the RNG), keeping the cooldown draw part of the pinned sequence.
    private static final CooldownRange SHORT_COOLDOWN = CooldownRange.ofSeconds(15, 30);
    private static final CooldownRange MEDIUM_COOLDOWN = CooldownRange.ofSeconds(30, 60);

    private static final BehaviorPlanningMetadata WORK_HEAVY_DESCRIPTOR =
            descriptor(WORK_HEAVY, BehaviorCategory.WORK, WorkIntensity.HEAVY, 20, MEDIUM_COOLDOWN);
    private static final BehaviorPlanningMetadata WORK_LIGHT_DESCRIPTOR =
            descriptor(WORK_LIGHT, BehaviorCategory.WORK, WorkIntensity.LIGHT, 15, SHORT_COOLDOWN);
    private static final BehaviorPlanningMetadata SOCIAL_DESCRIPTOR =
            descriptor(SOCIAL, BehaviorCategory.SOCIAL, WorkIntensity.NONE, 10, SHORT_COOLDOWN);
    private static final BehaviorPlanningMetadata SELF_CARE_DESCRIPTOR =
            descriptor(SELF_CARE, BehaviorCategory.SELF_CARE, WorkIntensity.NONE, 8, SHORT_COOLDOWN);
    private static final BehaviorPlanningMetadata LEISURE_DESCRIPTOR =
            descriptor(LEISURE, BehaviorCategory.LEISURE, WorkIntensity.NONE, 12, MEDIUM_COOLDOWN);

    @Test
    void farmerWorkDay_isDeterministicAndMatchesPinnedBaseline() {
        // Arrange
        VillagerProfessionKey profession = VillagerProfessionKey.FARMER;
        PlanDayType dayType = PlanDayType.WORK_DAY;
        long wakeAtAbsoluteTick = ScheduleProfile.defaultFor(profession).defaultWakeTick();
        long chronotypeSeed = 11L;
        long planSeed = 101L;
        List<WeightedBehavior> behaviors = farmerPool();

        // Act
        DayPlan planA = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));
        DayPlan planB = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));

        // Assert — determinism: two independently-built, identically-seeded contexts must produce the identical plan
        assertEquals(renderSlots(planA), renderSlots(planB), "Same planSeed must reproduce the identical slot sequence");
        assertEquals(renderSchedule(planA.getSchedule()), renderSchedule(planB.getSchedule()), "Same planSeed must reproduce the identical schedule");

        // Assert — pinned baseline (see class javadoc for the sweep contract these literals protect).
        // P3a re-pin: lunch/dinner moved to 11:00/16:30, MORNING no longer truncates at lunch (the
        // lunch anchor now splits it into a before/after free sub-interval, each repacked from the
        // band's own basePriority — hence the second "prio=70" run right after the lunch anchor
        // below), and a new EVENING band (prio 40) appears after dinner.
        assertEquals(List.of(
                "eat_food @4500 prio=100 dur=133 rigid",
                "golden_work_light @5545 prio=70 dur=250 flex",
                "golden_work_heavy @5733 prio=69 dur=333 flex",
                "golden_work_light @6104 prio=68 dur=250 flex",
                "golden_work_light @6774 prio=67 dur=250 flex",
                "golden_work_heavy @6962 prio=66 dur=333 flex",
                "golden_work_light @7366 prio=65 dur=250 flex",
                "golden_work_light @7739 prio=64 dur=250 flex",
                "golden_work_heavy @8038 prio=63 dur=333 flex",
                "golden_work_light @8288 prio=62 dur=250 flex",
                "golden_work_light @8987 prio=61 dur=250 flex",
                "golden_work_heavy @9175 prio=60 dur=333 flex",
                "golden_work_light @9533 prio=59 dur=250 flex",
                "golden_work_heavy @9875 prio=58 dur=333 flex",
                "golden_work_light @10125 prio=57 dur=250 flex",
                "golden_work_light @10560 prio=56 dur=250 flex",
                "golden_work_heavy @10748 prio=55 dur=333 flex",
                "eat_food @11155 prio=95 dur=166 rigid",
                "golden_work_heavy @11323 prio=70 dur=333 flex",
                "golden_work_light @11573 prio=69 dur=250 flex",
                "golden_work_light @12160 prio=68 dur=250 flex",
                "golden_work_heavy @12348 prio=67 dur=333 flex",
                "golden_work_light @12714 prio=66 dur=250 flex",
                "golden_work_heavy @13208 prio=65 dur=333 flex",
                "golden_work_light @13458 prio=64 dur=250 flex",
                "golden_work_heavy @13917 prio=63 dur=333 flex",
                "golden_work_light @14167 prio=62 dur=250 flex",
                "golden_work_light @14511 prio=61 dur=250 flex",
                "golden_work_light @15107 prio=50 dur=250 flex",
                "golden_self_care @15273 prio=49 dur=133 flex",
                "golden_social @15439 prio=48 dur=166 flex",
                "golden_work_light @15605 prio=47 dur=250 flex",
                "golden_self_care @15862 prio=46 dur=133 flex",
                "golden_social @16028 prio=45 dur=166 flex",
                "golden_work_light @16194 prio=44 dur=250 flex",
                "golden_self_care @16360 prio=43 dur=133 flex",
                "eat_food @16655 prio=90 dur=166 rigid",
                "golden_self_care @16860 prio=40 dur=133 flex",
                "golden_social @17026 prio=39 dur=166 flex",
                "golden_self_care @17405 prio=38 dur=133 flex"
        ), renderSlots(planA));
        assertEquals("wake=4500 bedtime=18209 blocks=[IDLE:4500-5500,WORK:5500-15000,MEET:15000-18209]", renderSchedule(planA.getSchedule()));
    }

    @Test
    void farmerRestDay_isDeterministicAndMatchesPinnedBaseline() {
        // Arrange
        VillagerProfessionKey profession = VillagerProfessionKey.FARMER;
        PlanDayType dayType = PlanDayType.REST_DAY;
        long wakeAtAbsoluteTick = ScheduleProfile.defaultFor(profession).defaultWakeTick();
        long chronotypeSeed = 12L;
        long planSeed = 102L;
        List<WeightedBehavior> behaviors = farmerPool();

        // Act
        DayPlan planA = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));
        DayPlan planB = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));

        // Assert — determinism
        assertEquals(renderSlots(planA), renderSlots(planB), "Same planSeed must reproduce the identical slot sequence");
        assertEquals(renderSchedule(planA.getSchedule()), renderSchedule(planB.getSchedule()), "Same planSeed must reproduce the identical schedule");

        // Assert — pinned baseline. P3a re-pin: lunch/dinner moved to 11:00/16:30, MORNING's own
        // end moved with the lunch bound (rest/nitwit MORNING ends near the lunch anchor), and the
        // schedule's IDLE->MEET pivot now follows the lunch anchor (~11:30) instead of 13:00.
        assertEquals(List.of(
                "eat_food @4500 prio=100 dur=133 rigid",
                "golden_work_light @5268 prio=55 dur=250 flex",
                "golden_social @5434 prio=54 dur=166 flex",
                "golden_work_heavy @5600 prio=53 dur=333 flex",
                "golden_self_care @5783 prio=52 dur=133 flex",
                "golden_work_light @5949 prio=51 dur=250 flex",
                "golden_social @6115 prio=50 dur=166 flex",
                "golden_self_care @6281 prio=49 dur=133 flex",
                "golden_work_heavy @6447 prio=48 dur=333 flex",
                "golden_social @6630 prio=47 dur=166 flex",
                "golden_work_light @6796 prio=46 dur=250 flex",
                "golden_self_care @6962 prio=45 dur=133 flex",
                "golden_work_heavy @7128 prio=44 dur=333 flex",
                "golden_social @7311 prio=43 dur=166 flex",
                "golden_work_light @7477 prio=42 dur=250 flex",
                "golden_self_care @7643 prio=41 dur=133 flex",
                "golden_work_heavy @7809 prio=40 dur=333 flex",
                "golden_social @7992 prio=39 dur=166 flex",
                "golden_work_light @8158 prio=38 dur=250 flex",
                "golden_self_care @8324 prio=37 dur=133 flex",
                "golden_social @8575 prio=36 dur=166 flex",
                "golden_work_light @8741 prio=35 dur=250 flex",
                "golden_self_care @8907 prio=34 dur=133 flex",
                "golden_social @9073 prio=33 dur=166 flex",
                "golden_work_heavy @9239 prio=32 dur=333 flex",
                "golden_work_light @9422 prio=31 dur=250 flex",
                "golden_social @9588 prio=30 dur=166 flex",
                "golden_self_care @9754 prio=29 dur=133 flex",
                "golden_work_light @9920 prio=28 dur=250 flex",
                "golden_social @10086 prio=27 dur=166 flex",
                "golden_self_care @10252 prio=26 dur=133 flex",
                "eat_food @10959 prio=95 dur=166 rigid",
                "golden_social @11640 prio=50 dur=166 flex",
                "golden_self_care @11806 prio=49 dur=133 flex",
                "golden_work_light @11972 prio=48 dur=250 flex",
                "golden_social @12138 prio=47 dur=166 flex",
                "golden_self_care @12304 prio=46 dur=133 flex",
                "golden_work_light @12470 prio=45 dur=250 flex",
                "golden_social @12793 prio=44 dur=166 flex",
                "golden_self_care @12959 prio=43 dur=133 flex",
                "golden_work_light @13125 prio=42 dur=250 flex",
                "golden_social @13291 prio=41 dur=166 flex",
                "golden_work_light @13593 prio=40 dur=250 flex",
                "golden_self_care @13759 prio=39 dur=133 flex",
                "golden_social @13925 prio=38 dur=166 flex",
                "golden_work_light @14241 prio=37 dur=250 flex",
                "golden_social @14407 prio=36 dur=166 flex",
                "golden_self_care @14573 prio=35 dur=133 flex",
                "golden_work_light @14739 prio=34 dur=250 flex",
                "golden_social @14905 prio=33 dur=166 flex",
                "golden_self_care @15071 prio=32 dur=133 flex",
                "golden_social @15237 prio=31 dur=166 flex",
                "golden_work_light @15403 prio=30 dur=250 flex",
                "golden_self_care @15729 prio=29 dur=133 flex",
                "golden_social @15895 prio=28 dur=166 flex",
                "golden_work_light @16061 prio=27 dur=250 flex",
                "eat_food @16459 prio=90 dur=166 rigid",
                "golden_social @16652 prio=40 dur=166 flex",
                "golden_self_care @16818 prio=39 dur=133 flex",
                "golden_social @17197 prio=38 dur=166 flex",
                "golden_self_care @17363 prio=37 dur=133 flex",
                "golden_social @17833 prio=36 dur=166 flex",
                "golden_self_care @17999 prio=35 dur=133 flex"
        ), renderSlots(planA));
        assertEquals("wake=4500 bedtime=19059 blocks=[IDLE:4500-11500,MEET:11500-19059]", renderSchedule(planA.getSchedule()));
    }

    @Test
    void fishermanWorkDay_isDeterministicAndMatchesPinnedBaseline() {
        // Arrange
        VillagerProfessionKey profession = VillagerProfessionKey.FISHERMAN;
        PlanDayType dayType = PlanDayType.WORK_DAY;
        long wakeAtAbsoluteTick = ScheduleProfile.defaultFor(profession).defaultWakeTick();
        long chronotypeSeed = 21L;
        long planSeed = 201L;
        List<WeightedBehavior> behaviors = fishermanPool();

        // Act
        DayPlan planA = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));
        DayPlan planB = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));

        // Assert — determinism
        assertEquals(renderSlots(planA), renderSlots(planB), "Same planSeed must reproduce the identical slot sequence");
        assertEquals(renderSchedule(planA.getSchedule()), renderSchedule(planB.getSchedule()), "Same planSeed must reproduce the identical schedule");

        // Assert — pinned baseline. P3a re-pin: lunch/dinner moved to 11:00/16:30. The old
        // truncate-at-lunch cap actually clamped the fisherman's MORNING band down to ~11:30 (workEnd
        // 13:00 vs the old maxEndCivil = noon - 500) even though the schedule's own WORK block already
        // said 5500-13000 — exactly the band/block drift the P3a migration note calls out. MORNING now
        // runs the full 5500-13000, so a second "prio=70" run reappears right after the lunch anchor.
        assertEquals(List.of(
                "eat_food @4000 prio=100 dur=133 rigid",
                "golden_work_heavy @5651 prio=70 dur=333 flex",
                "golden_work_light @5901 prio=69 dur=250 flex",
                "golden_work_heavy @6360 prio=68 dur=333 flex",
                "golden_work_light @6610 prio=67 dur=250 flex",
                "golden_work_light @7136 prio=66 dur=250 flex",
                "golden_work_heavy @7419 prio=65 dur=333 flex",
                "golden_work_light @7777 prio=64 dur=250 flex",
                "golden_work_heavy @8167 prio=63 dur=333 flex",
                "golden_work_light @8417 prio=62 dur=250 flex",
                "golden_work_light @9020 prio=61 dur=250 flex",
                "golden_work_heavy @9208 prio=60 dur=333 flex",
                "golden_work_light @9623 prio=59 dur=250 flex",
                "golden_work_heavy @10261 prio=58 dur=333 flex",
                "golden_work_light @10511 prio=57 dur=250 flex",
                "eat_food @11002 prio=95 dur=166 rigid",
                "golden_work_heavy @11294 prio=70 dur=333 flex",
                "golden_work_light @11544 prio=69 dur=250 flex",
                "golden_work_light @12108 prio=68 dur=250 flex",
                "golden_work_heavy @12296 prio=67 dur=333 flex",
                "golden_work_light @12693 prio=66 dur=250 flex",
                "golden_social @13091 prio=50 dur=166 flex",
                "golden_self_care @13257 prio=49 dur=133 flex",
                "golden_work_light @13423 prio=48 dur=250 flex",
                "golden_self_care @13702 prio=47 dur=133 flex",
                "golden_work_light @13868 prio=46 dur=250 flex",
                "golden_social @14034 prio=45 dur=166 flex",
                "golden_self_care @14200 prio=44 dur=133 flex",
                "golden_work_light @14509 prio=43 dur=250 flex",
                "golden_social @14675 prio=42 dur=166 flex",
                "golden_self_care @14841 prio=41 dur=133 flex",
                "golden_work_light @15007 prio=40 dur=250 flex",
                "golden_social @15173 prio=39 dur=166 flex",
                "golden_self_care @15339 prio=38 dur=133 flex",
                "golden_work_light @15505 prio=37 dur=250 flex",
                "golden_self_care @15671 prio=36 dur=133 flex",
                "golden_social @15837 prio=35 dur=166 flex",
                "golden_work_light @16003 prio=34 dur=250 flex",
                "golden_self_care @16169 prio=33 dur=133 flex",
                "eat_food @16502 prio=90 dur=166 rigid",
                "golden_self_care @16700 prio=40 dur=133 flex",
                "golden_social @16866 prio=39 dur=166 flex",
                "golden_self_care @17032 prio=38 dur=133 flex",
                "golden_social @17410 prio=37 dur=166 flex",
                "golden_self_care @17576 prio=36 dur=133 flex"
        ), renderSlots(planA));
        assertEquals("wake=4000 bedtime=18277 blocks=[IDLE:4000-5500,WORK:5500-13000,MEET:13000-18277]", renderSchedule(planA.getSchedule()));
    }

    @Test
    void nitwitRestDay_isDeterministicAndMatchesPinnedBaseline() {
        // Arrange
        VillagerProfessionKey profession = VillagerProfessionKey.NITWIT;
        PlanDayType dayType = PlanDayType.REST_DAY;
        long wakeAtAbsoluteTick = ScheduleProfile.defaultFor(profession).defaultWakeTick();
        long chronotypeSeed = 31L;
        long planSeed = 301L;
        List<WeightedBehavior> behaviors = nitwitPool();

        // Act
        DayPlan planA = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));
        DayPlan planB = this.generator.generate(context(profession, dayType, wakeAtAbsoluteTick, chronotypeSeed, planSeed, behaviors));

        // Assert — determinism
        assertEquals(renderSlots(planA), renderSlots(planB), "Same planSeed must reproduce the identical slot sequence");
        assertEquals(renderSchedule(planA.getSchedule()), renderSchedule(planB.getSchedule()), "Same planSeed must reproduce the identical schedule");

        // Assert — pinned baseline. P3a re-pin: lunch/dinner moved to 11:00/16:30, rest/nitwit
        // MORNING ends near the (earlier) lunch anchor, the schedule's IDLE->MEET pivot now follows
        // lunch (~11:30) instead of 13:00, and a new post-dinner EVENING band appears.
        assertEquals(List.of(
                "eat_food @9000 prio=100 dur=133 rigid",
                "golden_leisure @9757 prio=55 dur=200 flex",
                "golden_social @9923 prio=54 dur=166 flex",
                "golden_self_care @10089 prio=53 dur=133 flex",
                "eat_food @11024 prio=95 dur=166 rigid",
                "golden_leisure @11568 prio=50 dur=200 flex",
                "golden_social @11734 prio=49 dur=166 flex",
                "golden_self_care @11900 prio=48 dur=133 flex",
                "golden_social @12327 prio=47 dur=166 flex",
                "golden_self_care @12493 prio=46 dur=133 flex",
                "golden_leisure @12659 prio=45 dur=200 flex",
                "golden_social @12825 prio=44 dur=166 flex",
                "golden_self_care @12991 prio=43 dur=133 flex",
                "golden_leisure @13504 prio=42 dur=200 flex",
                "golden_social @13670 prio=41 dur=166 flex",
                "golden_self_care @13836 prio=40 dur=133 flex",
                "golden_self_care @14215 prio=39 dur=133 flex",
                "golden_social @14381 prio=38 dur=166 flex",
                "golden_leisure @14547 prio=37 dur=200 flex",
                "golden_self_care @14713 prio=36 dur=133 flex",
                "golden_social @15027 prio=35 dur=166 flex",
                "golden_self_care @15193 prio=34 dur=133 flex",
                "golden_leisure @15486 prio=33 dur=200 flex",
                "golden_social @15652 prio=32 dur=166 flex",
                "golden_self_care @15818 prio=31 dur=133 flex",
                "golden_social @16080 prio=30 dur=166 flex",
                "golden_leisure @16246 prio=29 dur=200 flex",
                "eat_food @16524 prio=90 dur=166 rigid",
                "golden_leisure @16822 prio=40 dur=200 flex",
                "golden_social @16988 prio=39 dur=166 flex",
                "golden_self_care @17154 prio=38 dur=133 flex",
                "golden_social @17466 prio=37 dur=166 flex",
                "golden_self_care @17632 prio=36 dur=133 flex",
                "golden_leisure @17798 prio=35 dur=200 flex",
                "golden_self_care @18068 prio=34 dur=133 flex",
                "golden_social @18234 prio=33 dur=166 flex"
        ), renderSlots(planA));
        assertEquals("wake=9000 bedtime=20069 blocks=[IDLE:9000-11500,MEET:11500-20069]", renderSchedule(planA.getSchedule()));
    }

    private static List<WeightedBehavior> farmerPool() {
        return List.of(
                new WeightedBehavior(WORK_HEAVY_DESCRIPTOR, 3),
                new WeightedBehavior(WORK_LIGHT_DESCRIPTOR, 2),
                new WeightedBehavior(SOCIAL_DESCRIPTOR, 1),
                new WeightedBehavior(SELF_CARE_DESCRIPTOR, 1)
        );
    }

    private static List<WeightedBehavior> fishermanPool() {
        return List.of(
                new WeightedBehavior(WORK_HEAVY_DESCRIPTOR, 5),
                new WeightedBehavior(WORK_LIGHT_DESCRIPTOR, 1),
                new WeightedBehavior(SOCIAL_DESCRIPTOR, 1),
                new WeightedBehavior(SELF_CARE_DESCRIPTOR, 1)
        );
    }

    private static List<WeightedBehavior> nitwitPool() {
        return List.of(
                new WeightedBehavior(SOCIAL_DESCRIPTOR, 2),
                new WeightedBehavior(SELF_CARE_DESCRIPTOR, 1),
                new WeightedBehavior(LEISURE_DESCRIPTOR, 3)
        );
    }

    private static PlanGenerationContext context(VillagerProfessionKey profession, PlanDayType dayType,
                                                 long wakeAtAbsoluteTick, long chronotypeSeed, long planSeed,
                                                 List<WeightedBehavior> behaviors) {
        return PlanGenerationContext.builder()
                .profession(profession)
                .genetics(genetics(0.5, 0.5, 0.5, 0.5))
                .scheduleProfile(ScheduleProfile.defaultFor(profession))
                .restDayPolicy(RestDayPolicy.defaultFor(profession))
                .dayType(dayType)
                .availableBehaviors(behaviors)
                .wakeAtAbsoluteTick(wakeAtAbsoluteTick)
                .chronotypeSeed(chronotypeSeed)
                .planSeed(planSeed)
                .build();
    }

    private static GeneticsProfile genetics(double constitution, double will, double agility, double charisma) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        genes.put(GeneType.CONSTITUTION, new Gene(constitution));
        genes.put(GeneType.WILL, new Gene(will));
        genes.put(GeneType.AGILITY, new Gene(agility));
        genes.put(GeneType.CHARISMA, new Gene(charisma));
        return new GeneticsProfile(genes);
    }

    private static BehaviorPlanningMetadata descriptor(BehaviorKey key, BehaviorCategory category, WorkIntensity intensity,
                                                       int durationMinutes, CooldownRange cooldown) {
        return BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(category)
                .intensity(intensity)
                .estimatedDuration(GameTicks.minutes(durationMinutes))
                .preconditionSummary("golden-test fixture")
                .cooldown(cooldown)
                .build();
    }

    /**
     * Renders a plan's slots as one readable line per slot — easier to diff on assertion
     * failure than comparing {@link PlanSlot} objects field-by-field.
     */
    private static List<String> renderSlots(DayPlan plan) {
        return plan.getSlots().stream()
                .map(DayPlanGoldenTest::renderSlot)
                .toList();
    }

    private static String renderSlot(PlanSlot slot) {
        return "%s @%d prio=%d dur=%d %s".formatted(
                slot.getBehaviorKey().id(),
                slot.getStartTick(),
                slot.getPriority(),
                slot.getEstimatedDurationTicks(),
                slot.isFlexible() ? "flex" : "rigid");
    }

    private static String renderSchedule(DayPlanSchedule schedule) {
        String blocks = schedule.activityBlocks().stream()
                .map(block -> "%s:%d-%d".formatted(block.context(), block.startTick(), block.endTick()))
                .collect(Collectors.joining(","));
        return "wake=%d bedtime=%d blocks=[%s]".formatted(schedule.wakeTick(), schedule.bedtimeTick(), blocks);
    }

}
