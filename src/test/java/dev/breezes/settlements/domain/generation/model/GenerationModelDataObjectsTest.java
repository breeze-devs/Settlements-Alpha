package dev.breezes.settlements.domain.generation.model;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.building.DisplayInfo;
import dev.breezes.settlements.domain.generation.model.building.GlobalAffinity;
import dev.breezes.settlements.domain.generation.model.building.ProximityAffinity;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.Plot;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationModelDataObjectsTest {

    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");
    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");
    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");

    @Test
    void terrainGrid_worldQueriesSnapToNearestGridPoint() {
        TerrainGrid grid = TerrainGrid.of(100, 200, 4, new TerrainSample[][]{
                {new TerrainSample(64, BiomeId.of("minecraft:plains"), 0.8f), new TerrainSample(66, BiomeId.of("minecraft:plains"), 0.8f)},
                {new TerrainSample(70, BiomeId.of("minecraft:river"), 0.5f), new TerrainSample(72, BiomeId.of("minecraft:river"), 0.5f)}
        });

        assertEquals(0, grid.worldToGridX(101));
        assertEquals(1, grid.worldToGridX(103));
        assertEquals(1, grid.worldToGridZ(204));
        assertEquals(72, grid.getHeightAtWorld(104, 204));
        assertEquals(BiomeId.of("minecraft:river"), grid.getAtWorld(104, 204).biome());
    }

    @Test
    void surveyBounds_fromBuildAreaExpandsSampleArea() {
        BoundingRegion buildArea = BoundingRegion.of(new BlockPosition(10, 64, 10), new BlockPosition(20, 70, 20));

        SurveyBounds bounds = SurveyBounds.fromBuildArea(buildArea, 8);

        assertEquals(buildArea, bounds.buildArea());
        assertEquals(BoundingRegion.of(new BlockPosition(2, 64, 2), new BlockPosition(28, 70, 28)), bounds.sampleArea());
        assertEquals(8, bounds.padding());
    }

    @Test
    void settlementProfile_traitHelpersWorkAsExpected() {
        SettlementProfile profile = new SettlementProfile(
                FARMING,
                List.of(FISHING),
                List.of(DEFENSE),
                Map.of(FARMING, 1.0f),
                ScaleTier.VILLAGE,
                42,
                0.6f,
                DefenseLevel.MODERATE,
                99L,
                List.of()
        );

        assertTrue(profile.hasTrait(FARMING));
        assertTrue(profile.hasTrait(FISHING));
        assertFalse(profile.hasTrait(MINING));
        assertEquals(TraitSlot.PRIMARY, profile.getTraitSlot(FARMING));
        assertEquals(TraitSlot.SECONDARY, profile.getTraitSlot(FISHING));
        assertEquals(TraitSlot.FLAVOR, profile.getTraitSlot(DEFENSE));
        assertEquals(List.of(FARMING, FISHING, DEFENSE), profile.allTraits());
    }

    @Test
    void plotAndFootprintConstraintHelpersWorkAsExpected() {
        Plot plot = new Plot(
                1,
                ZoneTier.SUBURB,
                BoundingRegion.of(new BlockPosition(0, 64, 0), new BlockPosition(8, 68, 8)),
                Direction.SOUTH,
                65,
                2,
                true,
                Set.of(ResourceTag.FRESHWATER)
        );
        BuildingFootprint footprint = new BuildingFootprint(9, 11);

        assertEquals(ZoneTier.SUBURB, plot.zone());
        assertEquals(65, plot.targetY());
        assertTrue(plot.roadFrontage());
        assertTrue(plot.localResources().contains(ResourceTag.FRESHWATER));
        assertTrue(footprint.fits(8, 9));
        assertTrue(footprint.fits(1, 1));
        assertFalse(footprint.fits(10, 9));
    }

    @Test
    void buildingDefinitionAndAffinitiesAreUsableInTests() {
        BuildingDefinition building = new BuildingDefinition(
                "settlements:building_definitions/sawmill",
                new DisplayInfo("Sawmill", "Cuts timber", null, "minecraft:iron_axe"),
                Map.of(LUMBER, 0.8f),
                TraitSlot.SECONDARY,
                50,
                IntRange.of(3, 4),
                false,
                Set.of(ResourceTag.LUMBER),
                Set.of(),
                new BuildingFootprint(11, 13),
                Set.of("default"),
                List.of(new ProximityAffinity("settlements:building_definitions/log_storage", 0.5f, 0.8f)),
                List.of(new GlobalAffinity("trait:settlements:settlement_traits/lumber", 0.6f)),
                "settlements:building_definitions/lumberjack",
                1
        );

        assertFalse(building.isUniversal());
        assertEquals(50, building.placementPriority());
        assertTrue(building.zoneTierPreference().contains(4));
        assertTrue(building.footprint().fits(8, 10));
        assertEquals(0.5d, building.proximityAffinities().getFirst().bonusAtDistance(0), 0.0001d);
    }

    @Test
    void buildingManifestCopiesCollectionsDefensively() {
        BuildingDefinition house = new BuildingDefinition(
                "settlements:building_definitions/house",
                null,
                Map.of(),
                TraitSlot.FLAVOR,
                10,
                IntRange.of(0, 4),
                false,
                Set.of(),
                Set.of(),
                new BuildingFootprint(2, 2),
                Set.of(),
                List.of(),
                List.of(),
                null,
                0
        );
        List<BuildingDefinition> mutable = new ArrayList<>(List.of(house));

        BuildingManifest manifest = new BuildingManifest(mutable);
        mutable.clear();

        assertEquals(1, manifest.buildings().size());
        assertEquals("settlements:building_definitions/house", manifest.buildings().getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> manifest.buildings().add(house));
    }

    @Test
    void siteReportCopiesCollectionsDefensively() {
        SiteReport report = new SiteReport(
                SurveyBounds.fromBuildArea(BoundingRegion.of(new BlockPosition(0, 64, 0), new BlockPosition(8, 68, 8)), 4),
                TerrainGrid.of(0, 0, 4, new TerrainSample[][]{{new TerrainSample(64, BiomeId.of("minecraft:plains"), 0.8f)}}),
                new ElevationStats(64, 64, 64, new BlockPosition(0, 64, 0)),
                Map.of(ResourceTag.FRESHWATER, 0.2f),
                Map.of(BiomeId.of("minecraft:plains"), 1.0f),
                Set.of(WaterFeatureType.RIVER),
                Set.of(ResourceTag.FRESHWATER)
        );

        assertEquals(1.0f, report.biomeDistribution().get(BiomeId.of("minecraft:plains")));
        assertEquals(0.2f, report.resourceDensities().get(ResourceTag.FRESHWATER));
        assertEquals(ResourceTag.FRESHWATER, report.resourceTags().iterator().next());
        assertThrows(UnsupportedOperationException.class, () -> report.resourceTags().add(ResourceTag.LUMBER));
    }
}
