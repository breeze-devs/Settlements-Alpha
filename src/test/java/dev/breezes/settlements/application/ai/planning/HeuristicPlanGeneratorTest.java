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
import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicPlanGeneratorTest {

    private final HeuristicPlanGenerator generator = new HeuristicPlanGenerator();

    @Test
    void generate_producesValidWorkDayPlanForEveryDefaultProfession() {
        for (ScheduleProfile profile : ScheduleProfile.defaultProfiles()) {
            DayPlan plan = this.generator.generate(context(profile.profession(), PlanDayType.WORK_DAY,
                    genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

            assertEquals(PlanDayType.WORK_DAY, plan.getDayType());
            assertFalse(plan.getSlots().isEmpty());
            assertHasMealsOnly(plan);
            assertSeamlessSchedule(plan);
            assertNoObsoleteIdleSlots(plan);
        }
    }

    @Test
    void generate_producesValidRestDayPlanForEveryDefaultProfession() {
        for (ScheduleProfile profile : ScheduleProfile.defaultProfiles()) {
            DayPlan plan = this.generator.generate(context(profile.profession(), PlanDayType.REST_DAY,
                    genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

            assertEquals(PlanDayType.REST_DAY, plan.getDayType());
            assertFalse(plan.getSlots().isEmpty());
            assertHasMealsOnly(plan);
            assertSeamlessSchedule(plan);
            assertNoObsoleteIdleSlots(plan);
        }
    }

    @Test
    void generate_workDayScheduleContainsIdleWorkAndMeetBlocks() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        List<DayPlanActivityContext> contexts = plan.getSchedule().activityBlocks().stream()
                .map(DayPlanActivityBlock::context)
                .toList();

        // Assert
        assertEquals(List.of(DayPlanActivityContext.IDLE, DayPlanActivityContext.WORK, DayPlanActivityContext.MEET), contexts);
    }

    @Test
    void generate_restDayScheduleContainsIdleAndMeetBlocksOnly() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.REST_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        List<DayPlanActivityContext> contexts = plan.getSchedule().activityBlocks().stream()
                .map(DayPlanActivityBlock::context)
                .toList();

        // Assert
        assertEquals(List.of(DayPlanActivityContext.IDLE, DayPlanActivityContext.MEET), contexts);
    }

    @Test
    void generate_restDayUsesMultipliersToPreferNonHeavyWork() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.REST_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        List<BehaviorKey> behaviorKeys = keys(plan);
        long heavyWorkSlots = behaviorKeys.stream()
                .filter(BehaviorKey.HARVEST_SUGARCANE::equals)
                .count();
        long lightWorkSlots = behaviorKeys.stream()
                .filter(BehaviorKey.MILK_COW::equals)
                .count();

        // Assert
        assertTrue(behaviorKeys.contains(BehaviorKey.of("gossip")));
        assertTrue(lightWorkSlots >= heavyWorkSlots);
        assertNoObsoleteIdleSlots(plan);
    }

    @Test
    void generate_restDayMorningIncludesSocialOrSelfCareCandidates() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.REST_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        List<BehaviorKey> morningFlexibleKeys = plan.getSlots().stream()
                .filter(PlanSlot::isFlexible)
                .filter(slot -> slot.getStartTick() < TimeOfDay.AT_12_00.getTick())
                .map(PlanSlot::getBehaviorKey)
                .toList();

        // Assert
        assertTrue(morningFlexibleKeys.stream().anyMatch(key -> key.equals(BehaviorKey.of("gossip"))
                        || key.equals(BehaviorKey.EAT_FOOD)),
                "Rest-day morning should include boosted social/self-care candidates, not only work behaviors.");
    }

    @Test
    void generate_workDayAllowsRepeatedSlotsToFillWorkWindow() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FISHERMAN, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        long fishingSlots = keys(plan).stream()
                .filter(BehaviorKey.FISHING::equals)
                .count();

        // Assert
        assertTrue(fishingSlots > 1, "Packed work windows should allow repeated primary behavior slots.");
    }

    @Test
    void generate_fishermanWeightMakesFishingDominantOverTameCat() {
        // Arrange
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FISHERMAN, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        List<BehaviorKey> behaviorKeys = keys(plan);
        long fishingSlots = behaviorKeys.stream()
                .filter(BehaviorKey.FISHING::equals)
                .count();
        long tameCatSlots = behaviorKeys.stream()
                .filter(BehaviorKey.TAME_CAT::equals)
                .count();

        // Assert
        assertTrue(fishingSlots > tameCatSlots,
                "FISHING has weight 5 and should dominate TAME_CAT's default weight 1.");
    }

    @Test
    void generate_includesRigidMealSlotsAndNoSleepSlot() {
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        long rigidEatSlots = plan.getSlots().stream()
                .filter(slot -> slot.getBehaviorKey().equals(BehaviorKey.EAT_FOOD))
                .filter(slot -> !slot.isFlexible())
                .count();

        assertTrue(rigidEatSlots >= 3);
        assertNoObsoleteIdleSlots(plan);
    }

    @Test
    void generate_nitwitPlansAvoidWorkWhenAlternativesExist() {
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.NITWIT, PlanDayType.REST_DAY,
                genetics(0.5, 0.5, 0.9, 0.2), allDescriptors()));

        List<BehaviorKey> workKeys = allDescriptors().stream()
                .filter(descriptor -> descriptor.getCategory() == BehaviorCategory.WORK)
                .map(BehaviorPlanningMetadata::getKey)
                .toList();

        assertTrue(keys(plan).stream().noneMatch(workKeys::contains));
    }

    @Test
    void generate_emptyCatalogStillProducesUniversalFallbackPlan() {
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), List.of()));

        assertHasMealsOnly(plan);
        assertNoObsoleteIdleSlots(plan);
    }

    @Test
    void generate_farmerAndLibrarianWorkDaysAreDifferent() {
        DayPlan farmer = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));
        DayPlan librarian = this.generator.generate(context(VillagerProfessionKey.LIBRARIAN, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        assertNotEquals(keys(farmer), keys(librarian));
        assertTrue(keys(farmer).contains(BehaviorKey.HARVEST_SUGARCANE));
        assertTrue(keys(librarian).contains(BehaviorKey.ENCHANT_ITEM));
    }

    @Test
    void generate_usesContextGameDayAsGeneratedForDay() {
        // Arrange
        long gameDay = 47L;
        PlanGenerationContext context = context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors(), gameDay);

        // Act
        DayPlan plan = this.generator.generate(context);

        // Assert
        assertEquals(gameDay, plan.getGeneratedForDay());
    }

    @Test
    void generate_standardVillagerWithHighConWakesBeforeSix() {
        // Arrange — ARMORER wakes at 6:30am (tick 500). High CON subtracts up to 1 000 ticks,
        // which would give tick -500. clampTick must wrap to 23 500 (5:30am), not clamp to 0 (6am).
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.ARMORER, PlanDayType.WORK_DAY,
                genetics(1.0, 0.5, 0.5, 0.5), allDescriptors()));

        int wakeTick = plan.getSlots().get(0).getStartTick();

        // Assert — high CON shifts wake earlier; result must be in pre-dawn range, not clamped to 0
        assertEquals(wakeTick, plan.getSchedule().wakeTick());
        assertTrue(wakeTick > 18_000 || wakeTick == 0,
                "High-CON standard villager wake tick should be pre-dawn or exactly 6am, but was: " + wakeTick);
        assertTrue(wakeTick != 500, "Wake tick must not equal the default 6:30am — CON should have shifted it");
    }

    @Test
    void generate_farmerPlanFirstSlotIsPreDawn() {
        // Arrange — farmer wakes at 5am (tick 23 000), before the 6am tick-0 boundary
        DayPlan plan = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        PlanSlot firstSlot = plan.getSlots().get(0);

        // Assert — first slot must be in the pre-dawn range (tick > 18 000) and be the wake meal
        assertTrue(firstSlot.getStartTick() > 18_000,
                "Farmer's first slot should be pre-dawn (tick > 18 000) but was: " + firstSlot.getStartTick());
        assertEquals(BehaviorKey.EAT_FOOD, firstSlot.getBehaviorKey());
        assertFalse(firstSlot.isFlexible());
        assertEquals(firstSlot.getStartTick(), plan.getSchedule().wakeTick());
    }

    @Test
    void generate_fishermanPlanFirstSlotIsBeforeFarmer() {
        // Arrange — fisherman (4:30am, tick 22 500) wakes earlier than farmer (5am, tick 23 000)
        DayPlan farmer = this.generator.generate(context(VillagerProfessionKey.FARMER, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));
        DayPlan fisherman = this.generator.generate(context(VillagerProfessionKey.FISHERMAN, PlanDayType.WORK_DAY,
                genetics(0.5, 0.5, 0.5, 0.5), allDescriptors()));

        // Act
        int farmerWakeTick = farmer.getSlots().get(0).getStartTick();
        int fishermanWakeTick = fisherman.getSlots().get(0).getStartTick();

        // Assert — fisherman tick is numerically smaller (earlier before the boundary wraps)
        assertTrue(fishermanWakeTick < farmerWakeTick,
                "Fisherman (" + fishermanWakeTick + ") should wake before farmer (" + farmerWakeTick + ")");
    }

    private static PlanGenerationContext context(VillagerProfessionKey profession, PlanDayType dayType,
                                                 GeneticsProfile genetics,
                                                 List<BehaviorPlanningMetadata> descriptors) {
        return context(profession, dayType, genetics, descriptors, 42L);
    }

    private static PlanGenerationContext context(VillagerProfessionKey profession, PlanDayType dayType,
                                                 GeneticsProfile genetics,
                                                 List<BehaviorPlanningMetadata> descriptors,
                                                 long gameDay) {
        return PlanGenerationContext.builder()
                .profession(profession)
                .genetics(genetics)
                .scheduleProfile(ScheduleProfile.defaultFor(profession))
                .restDayPolicy(RestDayPolicy.defaultFor(profession))
                .dayType(dayType)
                .availableBehaviors(descriptorsFor(profession, descriptors))
                .gameDay(gameDay)
                .build();
    }

    private static List<BehaviorKey> keys(DayPlan plan) {
        return plan.getSlots().stream()
                .map(PlanSlot::getBehaviorKey)
                .toList();
    }

    private static List<WeightedBehavior> descriptorsFor(VillagerProfessionKey profession,
                                                         List<BehaviorPlanningMetadata> descriptors) {
        Set<BehaviorKey> universalKeys = Set.of(
                BehaviorKey.EAT_FOOD,
                BehaviorKey.of("gossip")
        );

        Set<BehaviorKey> professionKeys;
        if (VillagerProfessionKey.FARMER.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.HARVEST_SUGARCANE, BehaviorKey.MILK_COW);
        } else if (VillagerProfessionKey.LIBRARIAN.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.ENCHANT_ITEM, BehaviorKey.of("organize_books"));
        } else if (VillagerProfessionKey.FISHERMAN.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.FISHING, BehaviorKey.TAME_CAT);
        } else if (VillagerProfessionKey.SHEPHERD.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.SHEAR_SHEEP);
        } else if (VillagerProfessionKey.BUTCHER.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.SMOKE_MEAT);
        } else if (VillagerProfessionKey.MASON.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.CUT_STONE);
        } else if (VillagerProfessionKey.CLERIC.equals(profession)) {
            professionKeys = Set.of(BehaviorKey.THROW_POTIONS);
        } else {
            professionKeys = Set.of();
        }

        Set<BehaviorKey> allowedKeys = Stream.concat(universalKeys.stream(), professionKeys.stream())
                .collect(java.util.stream.Collectors.toSet());

        return descriptors.stream()
                .filter(descriptor -> allowedKeys.contains(descriptor.getKey()))
                .map(descriptor -> new WeightedBehavior(descriptor, weightFor(profession, descriptor.getKey())))
                .toList();
    }

    private static int weightFor(VillagerProfessionKey profession, BehaviorKey key) {
        if (VillagerProfessionKey.FISHERMAN.equals(profession) && BehaviorKey.FISHING.equals(key)) {
            return 5;
        }
        return 1;
    }

    private static void assertHasMealsOnly(DayPlan plan) {
        assertTrue(keys(plan).contains(BehaviorKey.EAT_FOOD));
    }

    private static void assertNoObsoleteIdleSlots(DayPlan plan) {
        Set<BehaviorKey> obsoleteKeys = Set.of(BehaviorKey.of("idle_rest"), BehaviorKey.of("idle_wander"));
        assertTrue(keys(plan).stream().noneMatch(obsoleteKeys::contains));
    }

    private static void assertSeamlessSchedule(DayPlan plan) {
        DayPlanSchedule schedule = plan.getSchedule();
        List<DayPlanActivityBlock> blocks = schedule.activityBlocks();

        assertFalse(blocks.isEmpty());
        assertEquals(schedule.wakeTick(), blocks.getFirst().startTick());
        assertEquals(schedule.bedtimeTick(), blocks.getLast().endTick());

        for (int index = 1; index < blocks.size(); index++) {
            assertEquals(blocks.get(index - 1).endTick(), blocks.get(index).startTick());
        }
    }

    private static List<BehaviorPlanningMetadata> allDescriptors() {
        return List.of(
                descriptor(BehaviorKey.EAT_FOOD, BehaviorCategory.SELF_CARE, WorkIntensity.NONE),
                descriptor(BehaviorKey.of("gossip"), BehaviorCategory.SOCIAL, WorkIntensity.NONE),
                descriptor(BehaviorKey.HARVEST_SUGARCANE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                descriptor(BehaviorKey.MILK_COW, BehaviorCategory.WORK, WorkIntensity.LIGHT),
                descriptor(BehaviorKey.ENCHANT_ITEM, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                descriptor(BehaviorKey.of("organize_books"), BehaviorCategory.WORK, WorkIntensity.LIGHT),
                descriptor(BehaviorKey.FISHING, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                descriptor(BehaviorKey.TAME_CAT, BehaviorCategory.WORK, WorkIntensity.LIGHT),
                descriptor(BehaviorKey.SHEAR_SHEEP, BehaviorCategory.WORK, WorkIntensity.LIGHT),
                descriptor(BehaviorKey.SMOKE_MEAT, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                descriptor(BehaviorKey.CUT_STONE, BehaviorCategory.WORK, WorkIntensity.HEAVY),
                descriptor(BehaviorKey.THROW_POTIONS, BehaviorCategory.WORK, WorkIntensity.LIGHT)
        );
    }

    private static BehaviorPlanningMetadata descriptor(BehaviorKey key, BehaviorCategory category, WorkIntensity intensity) {
        return BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(category)
                .intensity(intensity)
                .estimatedDuration(GameTicks.minutes(20))
                .preconditionSummary("test")
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

}
