package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementValidatorTest {

    @Test
    void evaluate_validPlacement_returnsAcceptedWithResources() {
        TrackingScanner scanner = new TrackingScanner(Set.of(ResourceTag.FRESHWATER), Set.of());
        PlacementValidator validator = validator(scanner);
        LayoutSupport.CandidateFootprint footprint = flatFootprint(10, 10, 3, 3);

        PlacementResult result = validator.evaluate(LayoutTestFixtures.dock(), footprint);

        assertTrue(result.valid());
        assertNull(result.rejection());
        assertEquals(Set.of(ResourceTag.FRESHWATER), result.localResources());
        assertEquals(12, scanner.lastScanRadius);
    }

    @Test
    void evaluate_outsideBuildArea_returnsOutsideBuildArea() {
        PlacementValidator validator = validator(new TrackingScanner(Set.of(), Set.of()));
        LayoutSupport.CandidateFootprint footprint = flatFootprint(59, 59, 4, 4);

        PlacementResult result = validator.evaluate(LayoutTestFixtures.house(), footprint);

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.OUTSIDE_BUILD_AREA, result.rejection());
    }

    @Test
    void evaluate_tooSteep_returnsTooSteep() {
        PlacementValidator validator = validator(new TrackingScanner(Set.of(), Set.of()));
        LayoutSupport.CandidateFootprint footprint = new LayoutSupport.CandidateFootprint(
                BoundingRegion.of(new BlockPosition(10, 64, 10), new BlockPosition(12, 70, 12)),
                64,
                6,
                new LayoutSupport.BuildingFootprint(3, 3),
                Direction.NORTH,
                new BlockPosition(11, 64, 11)
        );

        PlacementResult result = validator.evaluate(LayoutTestFixtures.house(), footprint);

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.TOO_STEEP, result.rejection());
    }

    @Test
    void evaluate_gridConflict_returnsGridConflict() {
        TrackingScanner scanner = new TrackingScanner(Set.of(), Set.of());
        PlacementGrid grid = new PlacementGrid();
        LayoutSupport.CandidateFootprint footprint = flatFootprint(10, 10, 3, 3);
        grid.occupy(BoundingRegion.of(new BlockPosition(8, footprint.bounds().min().y(), 8), new BlockPosition(14, footprint.bounds().max().y(), 14)));
        PlacementValidator validator = new PlacementValidator(
                LayoutTestFixtures.standardReport().bounds().buildArea(),
                grid,
                LayoutTestFixtures.standardReport().terrainGrid(),
                scanner
        );

        PlacementResult result = validator.evaluate(LayoutTestFixtures.house(), footprint);

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.GRID_CONFLICT, result.rejection());
    }

    @Test
    void evaluate_missingRequiredResource_returnsMissingRequiredResource() {
        PlacementValidator validator = validator(new TrackingScanner(Set.of(), Set.of()));

        PlacementResult result = validator.evaluate(LayoutTestFixtures.mine(), flatFootprint(10, 10, 3, 3));

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.MISSING_REQUIRED_RESOURCE, result.rejection());
    }

    @Test
    void evaluate_forbiddenResourcePresent_returnsForbiddenResourcePresent() {
        TrackingScanner scanner = new TrackingScanner(Set.of(ResourceTag.STONE), Set.of());
        PlacementValidator validator = validator(scanner);

        PlacementResult result = validator.evaluate(
                LayoutTestFixtures.building(
                        "settlements:forbidden_stone",
                        Map.of(),
                        TraitSlot.FLAVOR,
                        10,
                        Set.of(),
                        Set.of(ResourceTag.STONE),
                        false,
                        4,
                        4,
                        4,
                        4
                ),
                flatFootprint(10, 10, 3, 3)
        );

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.FORBIDDEN_RESOURCE_PRESENT, result.rejection());
    }

    @Test
    void evaluate_centerOnWater_returnsOnWater() {
        TrackingScanner scanner = new TrackingScanner(Set.of(), Set.of(new XZ(10, 10)));
        PlacementValidator validator = validator(scanner);

        PlacementResult result = validator.evaluate(LayoutTestFixtures.house(), flatFootprint(10, 10, 3, 3));

        assertFalse(result.valid());
        assertEquals(PlacementResult.RejectionReason.ON_WATER, result.rejection());
    }

    @Test
    void evaluate_withPreScannedResources_skipsInternalScan() {
        TrackingScanner scanner = new TrackingScanner(Set.of(ResourceTag.STONE), Set.of());
        PlacementValidator validator = validator(scanner);

        PlacementResult result = validator.evaluate(LayoutTestFixtures.mine(), flatFootprint(10, 10, 3, 3), Set.of(ResourceTag.STONE));

        assertTrue(result.valid());
        assertEquals(0, scanner.scanCalls);
    }

    @Test
    void evaluate_scanRadiusIs12ForConstrainedAnd8ForUnconstrained() {
        TrackingScanner constrainedScanner = new TrackingScanner(Set.of(ResourceTag.STONE), Set.of());
        PlacementValidator constrainedValidator = validator(constrainedScanner);
        constrainedValidator.evaluate(LayoutTestFixtures.mine(), flatFootprint(10, 10, 3, 3));

        TrackingScanner unconstrainedScanner = new TrackingScanner(Set.of(), Set.of());
        PlacementValidator unconstrainedValidator = validator(unconstrainedScanner);
        unconstrainedValidator.evaluate(LayoutTestFixtures.house(), flatFootprint(20, 20, 3, 3));

        assertEquals(12, constrainedScanner.lastScanRadius);
        assertEquals(8, unconstrainedScanner.lastScanRadius);
    }

    private static PlacementValidator validator(TrackingScanner scanner) {
        return new PlacementValidator(
                LayoutTestFixtures.standardReport().bounds().buildArea(),
                new PlacementGrid(),
                LayoutTestFixtures.standardReport().terrainGrid(),
                scanner
        );
    }

    private static LayoutSupport.CandidateFootprint flatFootprint(int centerX, int centerZ, int width, int depth) {
        TerrainGrid grid = LayoutTestFixtures.standardReport().terrainGrid();
        BlockPosition center = new BlockPosition(centerX, grid.getHeightAtWorld(centerX, centerZ), centerZ);
        return LayoutSupport.evaluateFootprint(grid, center, Direction.NORTH, new LayoutSupport.BuildingFootprint(width, depth));
    }

    private static final class TrackingScanner extends LocalResourceScanner {

        private final Set<ResourceTag> scanResult;
        private final Set<XZ> waterPositions;
        private int scanCalls;
        private int lastScanRadius;

        private TrackingScanner(Set<ResourceTag> scanResult, Set<XZ> waterPositions) {
            super(LayoutTestFixtures.lookup());
            this.scanResult = scanResult;
            this.waterPositions = waterPositions;
        }

        @Override
        public Set<ResourceTag> scan(TerrainGrid grid, BlockPosition center, int radius) {
            this.scanCalls++;
            this.lastScanRadius = radius;
            return this.scanResult;
        }

        @Override
        public boolean isWaterAt(TerrainGrid grid, int worldX, int worldZ) {
            return this.waterPositions.contains(new XZ(worldX, worldZ));
        }
    }

    private record XZ(int x, int z) {
    }

}
