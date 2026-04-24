package dev.breezes.settlements.domain.generation.pipeline;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.generation.history.EventPreconditions;
import dev.breezes.settlements.domain.generation.history.HistoryEventDefinition;
import dev.breezes.settlements.domain.generation.history.HistoryEventRegistry;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.TraitDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.model.survey.TerrainSample;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.scoring.ConfiguredTraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerConfig;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerRegistry;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyData;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationPipelineTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");

    private final GenerationPipeline pipeline = new GenerationPipeline(
            lookup(),
            registry(),
            buildingRegistry(),
            emptyHistoryRegistry(),
            traitRegistry());

    @Test
    void flatPlainsPipeline_producesValidEndToEndResult() {
        TerrainGrid grid = gridFilled(64, 64, 64, BiomeId.of("minecraft:plains"));
        SurveyBounds bounds = boundsForGrid(64, 64);

        GenerationResult result = this.pipeline.generate(grid, bounds, 12345L);

        assertNotNull(result);
        assertTrue(result.siteReport().resourceTags().contains(ResourceTag.LUMBER)
                || result.siteReport().resourceDensities().containsKey(ResourceTag.LUMBER));
        assertNotNull(result.profile().primary());
        assertTrue(result.profile().historyEventIds().isEmpty());
        assertTrue(result.history().visualMarkers().isEmpty());
        assertTrue(insideBuildAreaXZ(bounds.buildArea(), result.layout().anchor()));
        assertFalse(result.layout().roads().isEmpty());
        assertFalse(result.layout().assignments().isEmpty());
    }

    @Test
    void historyEvent_isAppliedToProfileWeightsIdsAndVisualMarkers() {
        TerrainGrid grid = gridFilled(64, 64, 64, BiomeId.of("minecraft:plains"));
        SurveyBounds bounds = boundsForGrid(64, 64);
        GenerationResult baselineResult = this.pipeline.generate(grid, bounds, 12345L);
        GenerationPipeline pipelineWithHistory = new GenerationPipeline(
                lookup(),
                registry(),
                buildingRegistry(),
                singleEventHistoryRegistry(),
                traitRegistry());

        GenerationResult result = pipelineWithHistory.generate(grid, bounds, 12345L);

        assertEquals(List.of("settlements:settlement_events/founded_by_explorers"), result.profile().historyEventIds());
        assertEquals(List.of("settlements:settlement_events/founded_by_explorers"), result.history().eventIds());
        assertEquals(Set.of("explorer_camp", "flagpole"), result.history().visualMarkers().markers());
        assertEquals(
                baselineResult.profile().adjustedWeights().get(FARMING) + 0.10f,
                result.profile().adjustedWeights().get(FARMING));
        assertEquals(result.profile().adjustedWeights(), result.history().modifiedWeights());
    }

    @Test
    void emptyHistoryRegistry_isNoOpForHistoryFields() {
        TerrainGrid grid = gridFilled(64, 64, 64, BiomeId.of("minecraft:plains"));
        SurveyBounds bounds = boundsForGrid(64, 64);

        GenerationResult result = this.pipeline.generate(grid, bounds, 12345L);
        GenerationPipeline comparisonPipeline = new GenerationPipeline(
                lookup(),
                registry(),
                buildingRegistry(),
                emptyHistoryRegistry(),
                traitRegistry());
        GenerationResult comparison = comparisonPipeline.generate(grid, bounds, 12345L);

        assertTrue(result.profile().historyEventIds().isEmpty());
        assertTrue(result.history().eventIds().isEmpty());
        assertTrue(result.history().visualMarkers().isEmpty());
        assertEquals(comparison.profile().adjustedWeights(), result.profile().adjustedWeights());
        assertEquals(result.profile().adjustedWeights(), result.history().modifiedWeights());
    }

    @Test
    void sameSeed_sameInputs_producesIdenticalJson() {
        TerrainGrid grid = gridFilled(64, 64, 64, BiomeId.of("minecraft:plains"));
        SurveyBounds bounds = boundsForGrid(64, 64);

        GenerationResult first = this.pipeline.generate(grid, bounds, 24680L);
        GenerationResult second = this.pipeline.generate(grid, bounds, 24680L);

        assertEquals(domainSignature(first), domainSignature(second));
    }

    @Test
    void differentSeeds_produceDifferentJson() {
        TerrainGrid grid = gridFilled(64, 64, 64, BiomeId.of("minecraft:plains"));
        SurveyBounds bounds = boundsForGrid(64, 64);

        GenerationResult first = this.pipeline.generate(grid, bounds, 111L);
        GenerationResult second = this.pipeline.generate(grid, bounds, 222L);

        assertNotEquals(domainSignature(first), domainSignature(second));
    }

    private static BiomeSurveyLookup lookup() {
        return biomeId -> switch (biomeId.full()) {
            case "minecraft:plains" ->
                    new BiomeSurveyData(Map.of(ResourceTag.LUMBER, 0.1f, ResourceTag.FLORAL, 0.8f), null, Set.of());
            case "minecraft:river" ->
                    new BiomeSurveyData(Map.of(ResourceTag.FRESHWATER, 1.0f), WaterFeatureType.RIVER, Set.of());
            case "minecraft:stony_peaks" ->
                    new BiomeSurveyData(Map.of(ResourceTag.STONE, 0.9f, ResourceTag.ORE_BEARING, 0.7f), null, Set.of());
            default -> BiomeSurveyData.DEFAULT;
        };
    }

    private static TraitScorerRegistry registry() {
        return () -> {
            Map<TraitId, TraitScorer> scorers = new LinkedHashMap<>();
            scorers.put(LUMBER, new ConfiguredTraitScorer(new TraitScorerConfig(
                    LUMBER, 0.0f, Map.of(ResourceTag.LUMBER, 0.8f), Set.of(), Set.of(), Map.of(), Map.of(), 0.0f, 0.0f
            )));
            scorers.put(FARMING, new ConfiguredTraitScorer(new TraitScorerConfig(
                    FARMING, 0.15f, Map.of(ResourceTag.FLORAL, 0.6f, ResourceTag.FRESHWATER, 0.2f), Set.of(), Set.of(), Map.of(),
                    Map.of(BiomeId.of("minecraft:plains"), 0.2f), 0.0f, 0.0f
            )));
            scorers.put(FISHING, new ConfiguredTraitScorer(new TraitScorerConfig(
                    FISHING, 0.0f, Map.of(ResourceTag.FRESHWATER, 0.3f), Set.of(), Set.of(),
                    Map.of(WaterFeatureType.RIVER, 0.3f), Map.of(), 0.0f, 0.0f
            )));
            scorers.put(MINING, new ConfiguredTraitScorer(new TraitScorerConfig(
                    MINING, 0.0f, Map.of(ResourceTag.STONE, 0.4f, ResourceTag.ORE_BEARING, 0.35f), Set.of(), Set.of(), Map.of(), Map.of(), 0.2f, 60.0f
            )));
            scorers.put(DEFENSE, new ConfiguredTraitScorer(new TraitScorerConfig(
                    DEFENSE, 0.1f, Map.of(ResourceTag.STONE, 0.15f), Set.of(), Set.of(), Map.of(), Map.of(), 0.3f, 60.0f
            )));
            return scorers;
        };
    }

    private static BuildingRegistry buildingRegistry() {
        List<BuildingDefinition> buildings = List.of(
                building("settlements:building_definitions/town_hall", Map.of(), TraitSlot.FLAVOR, 1000, Set.of(), false, 9, 9),
                building("settlements:building_definitions/well", Map.of(), TraitSlot.FLAVOR, 900, Set.of(), false, 5, 5),
                building("settlements:building_definitions/tavern", Map.of(), TraitSlot.FLAVOR, 800, Set.of(), true, 8, 8),
                building("settlements:building_definitions/market_stall", Map.of(), TraitSlot.FLAVOR, 700, Set.of(), true, 5, 5),
                building("settlements:building_definitions/house", Map.of(), TraitSlot.FLAVOR, 10, Set.of(), true, 6, 6),
                building("settlements:building_definitions/farmhouse", Map.of(FARMING, 1.0f), TraitSlot.FLAVOR, 90, Set.of(), true, 7, 8),
                building("settlements:building_definitions/barn", Map.of(FARMING, 0.7f), TraitSlot.FLAVOR, 70, Set.of(), false, 7, 7),
                building("settlements:building_definitions/dock", Map.of(FISHING, 1.0f), TraitSlot.FLAVOR, 120, Set.of(ResourceTag.FRESHWATER), true, 8, 10),
                building("settlements:building_definitions/fish_drying_rack", Map.of(FISHING, 0.6f), TraitSlot.FLAVOR, 60, Set.of(), true, 5, 5),
                building("settlements:building_definitions/mine_entrance", Map.of(MINING, 1.0f), TraitSlot.FLAVOR, 115, Set.of(ResourceTag.STONE), false, 8, 8),
                building("settlements:building_definitions/smelter", Map.of(MINING, 0.7f), TraitSlot.FLAVOR, 75, Set.of(), false, 7, 7),
                building("settlements:building_definitions/watchtower", Map.of(DEFENSE, 1.0f), TraitSlot.FLAVOR, 85, Set.of(), false, 6, 6)
        );
        return new TestBuildingRegistry(buildings);
    }

    private static HistoryEventRegistry emptyHistoryRegistry() {
        return List::of;
    }

    private static HistoryEventRegistry singleEventHistoryRegistry() {
        return () -> List.of(HistoryEventDefinition.builder()
                .id("settlements:settlement_events/founded_by_explorers")
                .category("founding")
                .timeHorizonMin(100)
                .timeHorizonMax(100)
                .exclusiveTags(List.of("FOUNDING"))
                .probabilityWeight(1.0f)
                .preconditions(EventPreconditions.NONE)
                .traitModifiers(Map.of(FARMING, 0.10f))
                .visualMarkers(List.of("explorer_camp", "flagpole"))
                .narrativeText("An expedition party discovered this location and decided to stay.")
                .build());
    }

    private static TraitRegistry traitRegistry() {
        return new TraitRegistry() {
            private final Set<TraitId> traits = Set.of(LUMBER, FARMING, FISHING, MINING, DEFENSE);

            @Override
            public Set<TraitId> allTraitIds() {
                return this.traits;
            }

            @Override
            public Optional<TraitDefinition> byId(TraitId id) {
                return Optional.empty();
            }
        };
    }

    private static TerrainGrid gridFilled(int width, int depth, int height, BiomeId biomeId) {
        TerrainSample[][] samples = new TerrainSample[depth][width];
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                samples[z][x] = new TerrainSample(height, biomeId, 0.8f);
            }
        }
        return TerrainGrid.of(0, 0, 1, samples);
    }

    private static SurveyBounds boundsForGrid(int width, int depth) {
        return new SurveyBounds(
                BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(width - 1, 0, depth - 1)),
                BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(width - 1, 0, depth - 1)),
                0
        );
    }

    private static boolean insideBuildAreaXZ(BoundingRegion buildArea, BlockPosition position) {
        return position.x() >= buildArea.min().x()
                && position.x() <= buildArea.max().x()
                && position.z() >= buildArea.min().z()
                && position.z() <= buildArea.max().z();
    }

    private static String domainSignature(GenerationResult result) {
        return String.join("|",
                String.valueOf(result.generationSeed()),
                result.siteReport().elevation().toString(),
                result.siteReport().resourceDensities().toString(),
                result.siteReport().biomeDistribution().toString(),
                result.siteReport().waterFeatureTypes().toString(),
                result.siteReport().resourceTags().toString(),
                result.profile().toString(),
                result.layout().toString(),
                result.history().toString());
    }

    private static BuildingDefinition building(String id,
                                               Map<TraitId, Float> affinities,
                                               TraitSlot minimumRank,
                                               int priority,
                                               Set<ResourceTag> requiresResources,
                                               boolean roadFrontage,
                                               int width,
                                               int depth) {
        return new BuildingDefinition(
                id,
                null,
                affinities,
                minimumRank,
                priority,
                IntRange.of(0, 4),
                roadFrontage,
                requiresResources,
                Set.of(),
                new BuildingFootprint(width, depth),
                Set.of(),
                List.of(),
                List.of(),
                null,
                0
        );
    }

    private static final class TestBuildingRegistry implements BuildingRegistry {

        private final List<BuildingDefinition> buildings;
        private final Map<TraitId, List<BuildingDefinition>> byTrait;
        private final Map<String, BuildingDefinition> byId;

        private TestBuildingRegistry(List<BuildingDefinition> buildings) {
            this.buildings = List.copyOf(buildings);
            Map<TraitId, List<BuildingDefinition>> computedByTrait = new LinkedHashMap<>();
            for (BuildingDefinition definition : this.buildings) {
                for (TraitId trait : definition.traitAffinities().keySet()) {
                    computedByTrait.computeIfAbsent(trait, ignored -> new ArrayList<>())
                            .add(definition);
                }
            }
            this.byTrait = Map.copyOf(computedByTrait);
            this.byId = this.buildings.stream().collect(Collectors.toMap(BuildingDefinition::id, definition -> definition));
        }

        @Override
        public List<BuildingDefinition> allBuildings() {
            return this.buildings;
        }

        @Override
        public List<BuildingDefinition> constrainedBuildings() {
            return this.buildings.stream().filter(definition -> !definition.requiresResources().isEmpty()).toList();
        }

        @Override
        public List<BuildingDefinition> unconstrainedBuildings() {
            return this.buildings.stream().filter(definition -> definition.requiresResources().isEmpty()).toList();
        }

        @Override
        public List<BuildingDefinition> forTrait(TraitId trait) {
            return this.byTrait.getOrDefault(trait, List.of());
        }

        @Override
        public Optional<BuildingDefinition> byId(String id) {
            return Optional.ofNullable(this.byId.get(id));
        }
    }

}
