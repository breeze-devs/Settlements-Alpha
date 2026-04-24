package dev.breezes.settlements.domain.generation.building;

import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.building.FootprintConstraint;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingManifestCalculatorTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");
    private static final TraitId TRADE = TraitId.of("settlements:settlement_traits/trade");

    private static final BuildingDefinition TOWN_HALL = building("settlements:building_definitions/town_hall", Map.of(), TraitSlot.FLAVOR, 1000, Set.of());
    private static final BuildingDefinition WELL = building("settlements:building_definitions/well", Map.of(), TraitSlot.FLAVOR, 900, Set.of());
    private static final BuildingDefinition TAVERN = building("settlements:building_definitions/tavern", Map.of(), TraitSlot.FLAVOR, 800, Set.of());
    private static final BuildingDefinition MARKET_STALL = building("settlements:building_definitions/market_stall", Map.of(), TraitSlot.FLAVOR, 700, Set.of());
    private static final BuildingDefinition HOUSE = building("settlements:building_definitions/house", Map.of(), TraitSlot.FLAVOR, 10, Set.of());
    private static final BuildingDefinition SAWMILL = building("settlements:building_definitions/sawmill", Map.of(LUMBER, 1.0f), TraitSlot.FLAVOR, 120, Set.of(ResourceTag.LUMBER));
    private static final BuildingDefinition LOG_STORAGE = building("settlements:building_definitions/log_storage", Map.of(LUMBER, 0.8f), TraitSlot.FLAVOR, 80, Set.of());
    private static final BuildingDefinition FARMHOUSE = building("settlements:building_definitions/farmhouse", Map.of(FARMING, 1.0f), TraitSlot.FLAVOR, 90, Set.of());
    private static final BuildingDefinition BARN = building("settlements:building_definitions/barn", Map.of(FARMING, 0.7f), TraitSlot.FLAVOR, 70, Set.of());
    private static final BuildingDefinition DOCK = building("settlements:building_definitions/dock", Map.of(FISHING, 1.0f), TraitSlot.FLAVOR, 110, Set.of(ResourceTag.FRESHWATER));
    private static final BuildingDefinition DRYING_RACK = building("settlements:building_definitions/fish_drying_rack", Map.of(FISHING, 0.6f), TraitSlot.FLAVOR, 60, Set.of());
    private static final BuildingDefinition MINE = building("settlements:building_definitions/mine_entrance", Map.of(MINING, 1.0f), TraitSlot.FLAVOR, 115, Set.of(ResourceTag.STONE));
    private static final BuildingDefinition SMELTER = building("settlements:building_definitions/smelter", Map.of(MINING, 0.7f), TraitSlot.FLAVOR, 75, Set.of());
    private static final BuildingDefinition WATCHTOWER = building("settlements:building_definitions/watchtower", Map.of(DEFENSE, 1.0f), TraitSlot.FLAVOR, 100, Set.of());
    private static final BuildingDefinition BARRACKS = building("settlements:building_definitions/barracks", Map.of(DEFENSE, 0.8f), TraitSlot.SECONDARY, 85, Set.of());
    private static final BuildingDefinition PRIMARY_ONLY_LUMBER = building("settlements:building_definitions/primary_only_lumber", Map.of(LUMBER, 0.9f), TraitSlot.PRIMARY, 130, Set.of());

    private final BuildingRegistry registry = new TestBuildingRegistry(List.of(
            TOWN_HALL,
            WELL,
            TAVERN,
            MARKET_STALL,
            HOUSE,
            SAWMILL,
            LOG_STORAGE,
            FARMHOUSE,
            BARN,
            DOCK,
            DRYING_RACK,
            MINE,
            SMELTER,
            WATCHTOWER,
            BARRACKS,
            PRIMARY_ONLY_LUMBER
    ));

    private final BuildingManifestCalculator calculator = new BuildingManifestCalculator(this.registry);

    @Test
    void hamletSmallManifest() {
        SettlementProfile profile = profile(ScaleTier.HAMLET, LUMBER, List.of(), List.of());

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(101L));

        assertTrue(manifest.buildings().size() >= 5);
        assertTrue(manifest.buildings().size() <= 8);
        assertFalse(containsId(manifest, "settlements:building_definitions/tavern"));
    }

    @Test
    void villageIncludesCivics() {
        SettlementProfile profile = profile(ScaleTier.VILLAGE, LUMBER, List.of(FARMING), List.of(FISHING));

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(202L));

        assertTrue(containsId(manifest, "settlements:building_definitions/town_hall"));
        assertTrue(containsId(manifest, "settlements:building_definitions/tavern"));
    }

    @Test
    void townHasMoreBuildings() {
        SettlementProfile profile = profile(
                ScaleTier.TOWN,
                LUMBER,
                List.of(FARMING, FISHING),
                List.of(MINING, DEFENSE)
        );

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(303L));

        assertTrue(manifest.buildings().size() >= 25);
        assertTrue(manifest.buildings().size() <= 40);
    }

    @Test
    void primaryGetsMore() {
        SettlementProfile profile = profile(ScaleTier.VILLAGE, LUMBER, List.of(FARMING), List.of(FISHING));
        StubRandom random = new StubRandom()
                .withInt(0)
                .withInt(2)
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withDouble(0.00d)
                .withDouble(0.00d)
                .withDouble(0.00d)
                .withDouble(0.00d);

        BuildingManifest manifest = this.calculator.calculate(profile, random);

        long lumberCount = countIds(manifest, Set.of("settlements:building_definitions/sawmill", "settlements:building_definitions/log_storage"));
        long farmingCount = countIds(manifest, Set.of("settlements:building_definitions/farmhouse", "settlements:building_definitions/barn"));
        long fishingCount = countIds(manifest, Set.of("settlements:building_definitions/dock", "settlements:building_definitions/fish_drying_rack"));

        assertTrue(lumberCount > farmingCount);
        assertTrue(lumberCount > fishingCount);
    }

    @Test
    void housesFillRemainder() {
        SettlementProfile profile = profile(ScaleTier.HAMLET, LUMBER, List.of(), List.of());
        StubRandom random = new StubRandom()
                .withInt(0)
                .withInt(0)
                .withDouble(0.00d)
                .withDouble(0.00d);

        BuildingManifest manifest = this.calculator.calculate(profile, random);

        assertEquals(5, manifest.buildings().size());
        assertEquals(1, countIds(manifest, Set.of("settlements:building_definitions/house")));
    }

    @Test
    void constrainedSortedFirst() {
        SettlementProfile profile = profile(ScaleTier.VILLAGE, LUMBER, List.of(FISHING), List.of(MINING));

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(404L));

        boolean seenUnconstrained = false;
        for (BuildingDefinition definition : manifest.buildings()) {
            if (definition.requiresResources().isEmpty()) {
                seenUnconstrained = true;
                continue;
            }
            assertFalse(seenUnconstrained, "Found constrained building after unconstrained partition");
        }
    }

    @Test
    void withinPartitionSortedByPriority() {
        SettlementProfile profile = profile(ScaleTier.VILLAGE, LUMBER, List.of(FISHING), List.of(MINING));

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(505L));

        assertPartitionSortedDescending(manifest.buildings().stream().filter(def -> !def.requiresResources().isEmpty()).toList());
        assertPartitionSortedDescending(manifest.buildings().stream().filter(def -> def.requiresResources().isEmpty()).toList());
    }

    @Test
    void minimumRankFilters() {
        SettlementProfile profile = profile(ScaleTier.TOWN, FARMING, List.of(FISHING), List.of(LUMBER));
        StubRandom random = new StubRandom()
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withInt(0)
                .withDouble(0.00d)
                .withDouble(0.00d)
                .withDouble(0.00d)
                .withDouble(0.00d)
                .withDouble(0.00d);

        BuildingManifest manifest = this.calculator.calculate(profile, random);

        assertFalse(containsId(manifest, "settlements:building_definitions/primary_only_lumber"));
    }

    @Test
    void deterministic() {
        SettlementProfile profile = profile(ScaleTier.VILLAGE, LUMBER, List.of(FARMING), List.of(FISHING));

        BuildingManifest first = this.calculator.calculate(profile, new Random(606L));
        BuildingManifest second = this.calculator.calculate(profile, new Random(606L));

        assertEquals(
                first.buildings().stream().map(BuildingDefinition::id).toList(),
                second.buildings().stream().map(BuildingDefinition::id).toList()
        );
    }

    @Test
    void emptyTraitBuildings() {
        SettlementProfile profile = profile(ScaleTier.HAMLET, TRADE, List.of(), List.of());

        BuildingManifest manifest = assertDoesNotThrow(() -> this.calculator.calculate(profile, new Random(707L)));

        assertTrue(containsId(manifest, "settlements:building_definitions/town_hall"));
        assertTrue(containsId(manifest, "settlements:building_definitions/house"));
    }

    @Test
    void buildingCountWithinScale() {
        SettlementProfile profile = profile(
                ScaleTier.TOWN,
                LUMBER,
                List.of(FARMING, FISHING),
                List.of(MINING, DEFENSE)
        );

        BuildingManifest manifest = this.calculator.calculate(profile, new Random(808L));

        assertTrue(manifest.buildings().size() <= ScaleTier.TOWN.maxBuildings());
    }

    private static SettlementProfile profile(ScaleTier scaleTier,
                                             TraitId primary,
                                             List<TraitId> secondary,
                                             List<TraitId> flavor) {
        Map<TraitId, Float> adjustedWeights = new LinkedHashMap<>();
        adjustedWeights.put(primary, 1.0f);
        secondary.forEach(trait -> adjustedWeights.put(trait, 1.0f));
        flavor.forEach(trait -> adjustedWeights.put(trait, 1.0f));
        return new SettlementProfile(
                primary,
                secondary,
                flavor,
                adjustedWeights,
                scaleTier,
                20,
                0.5f,
                DefenseLevel.NONE,
                1L,
                List.of()
        );
    }

    private static BuildingDefinition building(String id,
                                               Map<TraitId, Float> traitAffinities,
                                               TraitSlot minimumRank,
                                               int placementPriority,
                                               Set<ResourceTag> requiresResources) {
        return new BuildingDefinition(
                id,
                null,
                traitAffinities,
                minimumRank,
                placementPriority,
                IntRange.of(0, 4),
                false,
                requiresResources,
                Set.of(),
                new FootprintConstraint(1, 2, 1, 2),
                Set.of(),
                List.of(),
                List.of(),
                null,
                0
        );
    }

    private static boolean containsId(BuildingManifest manifest, String id) {
        return manifest.buildings().stream().anyMatch(definition -> definition.id().equals(id));
    }

    private static long countIds(BuildingManifest manifest, Set<String> ids) {
        return manifest.buildings().stream().filter(definition -> ids.contains(definition.id())).count();
    }

    private static void assertPartitionSortedDescending(List<BuildingDefinition> partition) {
        List<Integer> actual = partition.stream().map(BuildingDefinition::placementPriority).toList();
        List<Integer> sorted = actual.stream().sorted(Comparator.reverseOrder()).toList();
        assertIterableEquals(sorted, actual);
    }

    private static final class TestBuildingRegistry implements BuildingRegistry {

        private final List<BuildingDefinition> allBuildings;

        private TestBuildingRegistry(List<BuildingDefinition> allBuildings) {
            this.allBuildings = List.copyOf(allBuildings);
        }

        @Override
        public List<BuildingDefinition> allBuildings() {
            return this.allBuildings;
        }

        @Override
        public List<BuildingDefinition> constrainedBuildings() {
            return this.allBuildings.stream().filter(definition -> !definition.requiresResources().isEmpty()).toList();
        }

        @Override
        public List<BuildingDefinition> unconstrainedBuildings() {
            return this.allBuildings.stream().filter(definition -> definition.requiresResources().isEmpty()).toList();
        }

        @Override
        public List<BuildingDefinition> forTrait(TraitId trait) {
            return this.allBuildings.stream().filter(definition -> definition.traitAffinities().containsKey(trait)).toList();
        }

        @Override
        public Optional<BuildingDefinition> byId(String id) {
            return this.allBuildings.stream().filter(definition -> definition.id().equals(id)).findFirst();
        }
    }

    private static final class StubRandom extends Random {

        private final List<Integer> ints = new ArrayList<>();
        private final List<Double> doubles = new ArrayList<>();

        private StubRandom withInt(int offsetFromOrigin) {
            this.ints.add(offsetFromOrigin);
            return this;
        }

        private StubRandom withDouble(double value) {
            this.doubles.add(value);
            return this;
        }

        @Override
        public int nextInt(int origin, int bound) {
            if (!this.ints.isEmpty()) {
                return origin + this.ints.removeFirst();
            }
            return super.nextInt(origin, bound);
        }

        @Override
        public double nextDouble(double bound) {
            if (!this.doubles.isEmpty()) {
                return this.doubles.removeFirst() * bound;
            }
            return super.nextDouble(bound);
        }
    }

}
