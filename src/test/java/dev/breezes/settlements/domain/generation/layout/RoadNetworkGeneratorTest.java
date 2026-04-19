package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.Plot;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadNetworkGeneratorTest {

    @Test
    void generateRoads_connectsNucleiAndProducesAnchorMainRoad() {
        RoadNetworkGenerator generator = new RoadNetworkGenerator();
        PlacementGrid grid = new PlacementGrid();
        BlockPosition planningCenter = new BlockPosition(20, 64, 20);
        List<BuildingAssignment> nuclei = List.of(
                assignmentAt(1, 10, 10),
                assignmentAt(2, 35, 20),
                assignmentAt(3, 25, 38)
        );

        List<RoadSegment> roads = generator.generateRoads(planningCenter, nuclei, LayoutTestFixtures.standardReport().terrainGrid(), grid, new Random(42L));

        assertFalse(roads.isEmpty());
        assertTrue(roads.stream().anyMatch(road -> road.type() == RoadType.MAIN && (planningCenter.sameXZ(road.start()) || planningCenter.sameXZ(road.end()))));
        assertTrue(roads.stream().anyMatch(road -> road.type() == RoadType.SECONDARY));
    }

    private static BuildingAssignment assignmentAt(int id, int x, int z) {
        Plot plot = new Plot(
                id,
                ZoneTier.DOWNTOWN,
                BoundingRegion.of(new BlockPosition(x, 64, z), new BlockPosition(x + 4, 68, z + 4)),
                Direction.SOUTH,
                64,
                0,
                false,
                Set.of()
        );
        return new BuildingAssignment(LayoutTestFixtures.house(), plot, Direction.SOUTH, null);
    }

}
