package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalResourceScannerTest {

    @Test
    void scan_detectsNearbyFreshwaterButNotDistantStone() {
        LocalResourceScanner scanner = new LocalResourceScanner(LayoutTestFixtures.lookup());

        Set<ResourceTag> nearRiver = scanner.scan(LayoutTestFixtures.standardReport().terrainGrid(), new BlockPosition(10, 64, 30), 6);
        Set<ResourceTag> riverArea = scanner.scan(LayoutTestFixtures.standardReport().terrainGrid(), new BlockPosition(10, 64, 30), 20);

        assertTrue(nearRiver.contains(ResourceTag.FRESHWATER));
        assertFalse(nearRiver.contains(ResourceTag.STONE));
        assertTrue(riverArea.contains(ResourceTag.FRESHWATER));
    }

    @Test
    void isWaterAt_detectsRiverBiomeSamples() {
        LocalResourceScanner scanner = new LocalResourceScanner(LayoutTestFixtures.lookup());
        TerrainGrid grid = LayoutTestFixtures.standardReport().terrainGrid();

        assertTrue(scanner.isWaterAt(grid, 10, 30));
        assertFalse(scanner.isWaterAt(grid, 20, 30));
    }

    @Test
    void isOnWater_allowsPartialWaterOnlyWhenCenterIsDry() {
        LocalResourceScanner scanner = new LocalResourceScanner(LayoutTestFixtures.lookup());
        TerrainGrid grid = LayoutTestFixtures.standardReport().terrainGrid();
        LayoutSupport.CandidateFootprint edgeTouchingFootprint = LayoutSupport.evaluateFootprint(
                grid,
                new BlockPosition(14, grid.getHeightAtWorld(14, 30), 30),
                Direction.NORTH,
                new BuildingFootprint(3, 3)
        );

        assertTrue(LayoutSupport.isOnWater(grid, scanner, edgeTouchingFootprint, false));
        assertFalse(LayoutSupport.isOnWater(grid, scanner, edgeTouchingFootprint, true));
    }

}
