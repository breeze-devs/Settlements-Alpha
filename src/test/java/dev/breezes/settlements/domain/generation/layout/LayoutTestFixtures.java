package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.IntRange;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.FootprintConstraint;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ElevationStats;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.model.survey.TerrainSample;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyData;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LayoutTestFixtures {

    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");
    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");

    private LayoutTestFixtures() {
    }

    static BiomeSurveyLookup lookup() {
        return biomeId -> switch (biomeId.full()) {
            case "minecraft:river" ->
                    new BiomeSurveyData(Map.of(ResourceTag.FRESHWATER, 1.0f), WaterFeatureType.RIVER, Set.of());
            case "minecraft:stony_peaks" ->
                    new BiomeSurveyData(Map.of(ResourceTag.STONE, 0.9f, ResourceTag.ORE_BEARING, 0.7f), null, Set.of());
            case "minecraft:forest" -> new BiomeSurveyData(Map.of(ResourceTag.LUMBER, 0.8f), null, Set.of());
            case "minecraft:plains" ->
                    new BiomeSurveyData(Map.of(ResourceTag.FRESHWATER, 0.0f, ResourceTag.STONE, 0.0f), null, Set.of());
            default -> BiomeSurveyData.DEFAULT;
        };
    }

    static SiteReport standardReport() {
        return reportWithSteepPatch(false);
    }

    static SiteReport steepReport() {
        return reportWithSteepPatch(true);
    }

    static SiteReport centerWaterReport() {
        TerrainSample[][] samples = new TerrainSample[60][60];
        for (int z = 0; z < 60; z++) {
            for (int x = 0; x < 60; x++) {
                BiomeId biome = BiomeId.of("minecraft:plains");
                int height = 64;
                if (x >= 26 && x <= 34 && z >= 26 && z <= 34) {
                    biome = BiomeId.of("minecraft:river");
                    height = 62;
                }
                samples[z][x] = new TerrainSample(height, biome, 0.8f);
            }
        }
        TerrainGrid grid = TerrainGrid.of(0, 0, 1, samples);
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(59, 0, 59));
        return new SiteReport(
                SurveyBounds.fromBuildArea(buildArea, 0),
                grid,
                new ElevationStats(62, 64, 64, new BlockPosition(0, 64, 0)),
                Map.of(ResourceTag.FRESHWATER, 0.1f),
                Map.of(
                        BiomeId.of("minecraft:plains"), 0.9f,
                        BiomeId.of("minecraft:river"), 0.1f
                ),
                Set.of(WaterFeatureType.RIVER),
                Set.of(ResourceTag.FRESHWATER)
        );
    }

    static SiteReport reportWithoutStone() {
        TerrainSample[][] samples = new TerrainSample[60][60];
        for (int z = 0; z < 60; z++) {
            for (int x = 0; x < 60; x++) {
                BiomeId biome = x >= 8 && x <= 13 ? BiomeId.of("minecraft:river") : BiomeId.of("minecraft:plains");
                int height = x >= 8 && x <= 13 ? 62 : 64;
                samples[z][x] = new TerrainSample(height, biome, 0.8f);
            }
        }
        TerrainGrid grid = TerrainGrid.of(0, 0, 1, samples);
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(59, 0, 59));
        return new SiteReport(
                SurveyBounds.fromBuildArea(buildArea, 0),
                grid,
                new ElevationStats(62, 64, 64, new BlockPosition(0, 64, 0)),
                Map.of(ResourceTag.FRESHWATER, 0.15f),
                Map.of(
                        BiomeId.of("minecraft:plains"), 0.9f,
                        BiomeId.of("minecraft:river"), 0.1f
                ),
                Set.of(WaterFeatureType.RIVER),
                Set.of(ResourceTag.FRESHWATER)
        );
    }

    static SiteReport stoneOnlyReport() {
        TerrainSample[][] samples = new TerrainSample[60][60];
        for (int z = 0; z < 60; z++) {
            for (int x = 0; x < 60; x++) {
                samples[z][x] = new TerrainSample(66, BiomeId.of("minecraft:stony_peaks"), 0.8f);
            }
        }
        TerrainGrid grid = TerrainGrid.of(0, 0, 1, samples);
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(59, 0, 59));
        return new SiteReport(
                SurveyBounds.fromBuildArea(buildArea, 0),
                grid,
                new ElevationStats(66, 66, 66, new BlockPosition(0, 66, 0)),
                Map.of(ResourceTag.STONE, 0.9f, ResourceTag.ORE_BEARING, 0.7f),
                Map.of(BiomeId.of("minecraft:stony_peaks"), 1.0f),
                Set.of(),
                Set.of(ResourceTag.STONE, ResourceTag.ORE_BEARING)
        );
    }

    private static SiteReport reportWithSteepPatch(boolean steep) {
        TerrainSample[][] samples = new TerrainSample[60][60];
        for (int z = 0; z < 60; z++) {
            for (int x = 0; x < 60; x++) {
                BiomeId biome = BiomeId.of("minecraft:plains");
                int height = 64;
                if (x >= 8 && x <= 13) {
                    biome = BiomeId.of("minecraft:river");
                    height = 62;
                }
                if (x >= 44 && x <= 54 && z >= 42 && z <= 54) {
                    biome = BiomeId.of("minecraft:stony_peaks");
                    height = steep ? 64 + ((x + z) % 8) : 66;
                }
                samples[z][x] = new TerrainSample(height, biome, 0.8f);
            }
        }
        TerrainGrid grid = TerrainGrid.of(0, 0, 1, samples);
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(59, 0, 59));
        return new SiteReport(
                SurveyBounds.fromBuildArea(buildArea, 0),
                grid,
                new ElevationStats(62, steep ? 71 : 66, 64, new BlockPosition(44, steep ? 71 : 66, 42)),
                Map.of(ResourceTag.FRESHWATER, 0.15f, ResourceTag.STONE, 0.12f),
                Map.of(
                        BiomeId.of("minecraft:plains"), 0.7f,
                        BiomeId.of("minecraft:river"), 0.1f,
                        BiomeId.of("minecraft:stony_peaks"), 0.2f
                ),
                Set.of(WaterFeatureType.RIVER),
                Set.of(ResourceTag.FRESHWATER, ResourceTag.STONE)
        );
    }

    static SettlementProfile villageProfile(long seed) {
        Map<TraitId, Float> weights = new LinkedHashMap<>();
        weights.put(FARMING, 1.0f);
        weights.put(FISHING, 0.8f);
        weights.put(MINING, 0.9f);
        weights.put(DEFENSE, 0.5f);
        return new SettlementProfile(
                FARMING,
                List.of(FISHING, MINING),
                List.of(DEFENSE),
                weights,
                ScaleTier.VILLAGE,
                40,
                0.5f,
                DefenseLevel.MODERATE,
                seed,
                List.of()
        );
    }

    static SiteReport townReport() {
        TerrainSample[][] samples = new TerrainSample[40][40];
        for (int z = 0; z < 40; z++) {
            for (int x = 0; x < 40; x++) {
                samples[z][x] = new TerrainSample(64, BiomeId.of("minecraft:plains"), 0.8f);
            }
        }
        TerrainGrid grid = TerrainGrid.of(0, 0, 4, samples);
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(0, 0, 0), new BlockPosition(156, 0, 156));
        return new SiteReport(
                SurveyBounds.fromBuildArea(buildArea, 0),
                grid,
                new ElevationStats(64, 64, 64, new BlockPosition(0, 64, 0)),
                Map.of(),
                Map.of(BiomeId.of("minecraft:plains"), 1.0f),
                Set.of(),
                Set.of()
        );
    }

    static SettlementProfile townProfile(long seed) {
        Map<TraitId, Float> weights = new LinkedHashMap<>();
        weights.put(FARMING, 1.0f);
        weights.put(FISHING, 0.8f);
        weights.put(MINING, 0.9f);
        weights.put(DEFENSE, 0.5f);
        weights.put(LUMBER, 0.7f);
        return new SettlementProfile(
                FARMING,
                List.of(FISHING, MINING),
                List.of(DEFENSE, LUMBER),
                weights,
                ScaleTier.TOWN,
                120,
                0.7f,
                DefenseLevel.HIGH,
                seed,
                List.of()
        );
    }

    static SettlementProfile hamletProfile(long seed) {
        Map<TraitId, Float> weights = new LinkedHashMap<>();
        weights.put(FARMING, 1.0f);
        return new SettlementProfile(
                FARMING,
                List.of(),
                List.of(),
                weights,
                ScaleTier.HAMLET,
                15,
                0.3f,
                DefenseLevel.NONE,
                seed,
                List.of()
        );
    }

    static BuildingDefinition townHall() {
        return building("settlements:building_definitions/town_hall", Map.of(), TraitSlot.FLAVOR, 1000, Set.of(), false, 7, 9, 7, 9);
    }

    static BuildingDefinition well() {
        return building("settlements:building_definitions/well", Map.of(), TraitSlot.FLAVOR, 900, Set.of(), false, 3, 5, 3, 5);
    }

    static BuildingDefinition dock() {
        return building("settlements:building_definitions/dock", Map.of(FISHING, 1.0f), TraitSlot.FLAVOR, 120, Set.of(ResourceTag.FRESHWATER), true, 6, 8, 8, 10);
    }

    static BuildingDefinition mine() {
        return building("settlements:building_definitions/mine_entrance", Map.of(MINING, 1.0f), TraitSlot.FLAVOR, 115, Set.of(ResourceTag.STONE), false, 6, 8, 6, 8);
    }

    static BuildingDefinition farmhouse() {
        return building("settlements:building_definitions/farmhouse", Map.of(FARMING, 1.0f), TraitSlot.FLAVOR, 90, Set.of(), true, 5, 7, 6, 8);
    }

    static BuildingDefinition watchtower() {
        return building("settlements:building_definitions/watchtower", Map.of(DEFENSE, 1.0f), TraitSlot.FLAVOR, 85, Set.of(), false, 4, 6, 4, 6);
    }

    static BuildingDefinition house() {
        return building("settlements:building_definitions/house", Map.of(), TraitSlot.FLAVOR, 10, Set.of(), true, 4, 6, 4, 6);
    }

    static BuildingDefinition building(String id,
                                       Map<TraitId, Float> affinities,
                                       TraitSlot minimumRank,
                                       int priority,
                                       Set<ResourceTag> requiresResources,
                                       boolean roadFrontage,
                                       int minWidth,
                                       int maxWidth,
                                       int minDepth,
                                       int maxDepth) {
        return building(id, affinities, minimumRank, priority, requiresResources, Set.of(), roadFrontage, minWidth, maxWidth, minDepth, maxDepth);
    }

    static BuildingDefinition building(String id,
                                       Map<TraitId, Float> affinities,
                                       TraitSlot minimumRank,
                                       int priority,
                                       Set<ResourceTag> requiresResources,
                                       Set<ResourceTag> forbiddenResources,
                                       boolean roadFrontage,
                                       int minWidth,
                                       int maxWidth,
                                       int minDepth,
                                       int maxDepth) {
        return new BuildingDefinition(
                id,
                null,
                affinities,
                minimumRank,
                priority,
                IntRange.of(0, 4),
                roadFrontage,
                requiresResources,
                forbiddenResources,
                new FootprintConstraint(minWidth, maxWidth, minDepth, maxDepth),
                Set.of(),
                List.of(),
                List.of(),
                null,
                0
        );
    }

}
