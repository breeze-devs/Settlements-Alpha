package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementLayoutEngineTest {

    private final SettlementLayoutEngine engine = new SettlementLayoutEngine();

    @Test
    void anchorWithinBuildArea_andAllAssignmentsInsideArea() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(123L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );

        BoundingRegion buildArea = LayoutTestFixtures.standardReport().bounds().buildArea();
        assertTrue(insideBuildAreaXZ(buildArea, result.anchor()));
        assertFalse(result.assignments().isEmpty());
        assertTrue(result.assignments().stream().allMatch(assignment -> insideBuildArea(buildArea, assignment.plot().bounds())));
        assertTrue(noOverlaps(result.assignments()));
    }

    @Test
    void constrainedBuildingsPlaceNearResources() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(321L),
                manifest(16),
                LayoutTestFixtures.lookup()
        );

        BuildingAssignment dock = findById(result.assignments(), "settlements:dock");
        BuildingAssignment mine = findById(result.assignments(), "settlements:mine_entrance");

        assertTrue(dock.plot().localResources().contains(ResourceTag.FRESHWATER));
        assertTrue(mine.plot().localResources().contains(ResourceTag.STONE));
    }

    @Test
    void constrainedBuildingsWithoutAvailableResourcesAreDroppedAfterAllPhases() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.reportWithoutStone(),
                LayoutTestFixtures.villageProfile(2468L),
                stoneConstrainedManifest(8),
                LayoutTestFixtures.lookup()
        );

        assertEquals(1, result.assignments().size());
        assertEquals("settlements:town_hall", result.assignments().getFirst().building().id());
    }

    @Test
    void forbiddenResourcesPreventPlacementAcrossAllPhases() {
        BuildingManifest manifest = new BuildingManifest(List.of(
                LayoutTestFixtures.townHall(),
                LayoutTestFixtures.building(
                        "settlements:forbidden_stone_building",
                        Map.of(),
                        TraitSlot.FLAVOR,
                        500,
                        Set.of(),
                        Set.of(ResourceTag.STONE),
                        false,
                        6,
                        8,
                        6,
                        8
                )
        ));

        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.stoneOnlyReport(),
                LayoutTestFixtures.villageProfile(1357L),
                manifest,
                LayoutTestFixtures.lookup()
        );

        assertEquals(1, result.assignments().size());
        assertEquals("settlements:town_hall", result.assignments().getFirst().building().id());
    }

    @Test
    void roadsConnectFromAnchor_andRoadFrontageAssignmentsTouchRoad() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(999L),
                manifest(22),
                LayoutTestFixtures.lookup()
        );

        BlockPosition planningCenter = planningCenter(result);
        assertTrue(result.roads().stream().anyMatch(road -> planningCenter.sameXZ(road.start()) || planningCenter.sameXZ(road.end())));
        assertTrue(allRoadFrontageAssignmentsTouchRoad(result));
    }

    @Test
    void hubCanPlaceAwayFromWaterloggedAnchor() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.centerWaterReport(),
                LayoutTestFixtures.villageProfile(2026L),
                manifest(12),
                LayoutTestFixtures.lookup()
        );

        BuildingAssignment townHall = findById(result.assignments(), "settlements:town_hall");
        BlockPosition planningCenter = planningCenter(result);

        assertFalse(result.anchor().sameXZ(planningCenter));
        assertFalse(result.anchor().sameXZ(townHall.plot().bounds().centerXZ()));
        assertEquals(ZoneTier.CORE, townHall.plot().zone());
    }

    @Test
    void hubUsesAnchorFirstWhenAnchorIsValid() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(777L),
                manifest(12),
                LayoutTestFixtures.lookup()
        );

        BuildingAssignment townHall = findById(result.assignments(), "settlements:town_hall");
        assertTrue(result.anchor().sameXZ(townHall.plot().bounds().centerXZ()));
    }

    @Test
    void determinism_sameSeedProducesSameLayout() {
        LayoutResult first = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(444L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );
        LayoutResult second = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(444L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );

        assertEquals(layoutSignature(first), layoutSignature(second));
    }

    @Test
    void allScaleTiersStayWithinBuildingEnvelopes() {
        LayoutResult hamlet = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.hamletProfile(17L),
                manifest(8),
                LayoutTestFixtures.lookup()
        );
        LayoutResult village = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(18L),
                manifest(20),  // VILLAGE maxBuildings = 20
                LayoutTestFixtures.lookup()
        );
        LayoutResult town = this.engine.generateLayout(
                LayoutTestFixtures.townReport(),
                LayoutTestFixtures.townProfile(19L),
                manifest(40),
                LayoutTestFixtures.lookup()
        );

        assertTrue(hamlet.assignments().size() >= ScaleTier.HAMLET.minBuildings());
        assertTrue(hamlet.assignments().size() <= ScaleTier.HAMLET.maxBuildings());
        assertTrue(village.assignments().size() >= ScaleTier.VILLAGE.minBuildings());
        assertTrue(village.assignments().size() <= ScaleTier.VILLAGE.maxBuildings());
        assertTrue(town.assignments().size() >= ScaleTier.TOWN.minBuildings());
        assertTrue(town.assignments().size() <= ScaleTier.TOWN.maxBuildings());
    }

    @Test
    void constrainedBuildingsUseDistanceBasedZones() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(321L),
                manifest(16),
                LayoutTestFixtures.lookup()
        );

        BuildingAssignment dock = findById(result.assignments(), "settlements:dock");
        BuildingAssignment mine = findById(result.assignments(), "settlements:mine_entrance");
        BlockPosition planningCenter = planningCenter(result);

        assertEquals(expectedZone(planningCenter, dock, ScaleTier.VILLAGE), dock.plot().zone());
        assertEquals(expectedZone(planningCenter, mine, ScaleTier.VILLAGE), mine.plot().zone());
    }

    @Test
    void minimumSpacingRespectedBetweenAssignments() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(555L),
                manifest(20),
                LayoutTestFixtures.lookup()
        );

        assertTrue(respectsMinimumSpacing(result.assignments(), PlacementGrid.DEFAULT_MINIMUM_SPACING));
    }

    @Test
    void zoneForDistance_increasesWithDistance() {
        assertEquals(ZoneTier.CORE, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 0.0d));
        assertEquals(ZoneTier.CORE, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 8.0d));
        assertEquals(ZoneTier.DOWNTOWN, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 9.0d));
        assertEquals(ZoneTier.DOWNTOWN, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 25.0d));
        assertEquals(ZoneTier.MIDTOWN, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 26.0d));
        assertEquals(ZoneTier.MIDTOWN, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 45.0d));
        assertEquals(ZoneTier.OUTER, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 46.0d));
        assertEquals(ZoneTier.OUTER, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 56.0d));
        assertEquals(ZoneTier.SUBURB, LayoutSupport.zoneForDistance(ScaleTier.VILLAGE, 57.0d));
    }

    @Test
    void allAssignmentsAvoidWaterAccordingToFootprintRules() {
        LayoutResult result = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(888L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );

        LocalResourceScanner scanner = new LocalResourceScanner(LayoutTestFixtures.lookup());
        assertTrue(result.assignments().stream().allMatch(assignment -> !LayoutSupport.isOnWater(
                LayoutTestFixtures.standardReport().terrainGrid(),
                scanner,
                new LayoutSupport.CandidateFootprint(
                        assignment.plot().bounds(),
                        assignment.plot().targetY(),
                        assignment.plot().maxElevationDelta(),
                        new LayoutSupport.BuildingFootprint(assignment.plot().bounds().widthX(), assignment.plot().bounds().widthZ()),
                        assignment.facing(),
                        assignment.plot().bounds().centerXZ().withY(assignment.plot().targetY())
                ),
                LayoutSupport.allowsPartialWater(assignment.building())
        )));
    }

    @Test
    void steepTerrainChangesLayoutOutcome() {
        LayoutResult flat = this.engine.generateLayout(
                LayoutTestFixtures.standardReport(),
                LayoutTestFixtures.villageProfile(4321L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );
        LayoutResult steep = this.engine.generateLayout(
                LayoutTestFixtures.steepReport(),
                LayoutTestFixtures.villageProfile(4321L),
                manifest(18),
                LayoutTestFixtures.lookup()
        );

        assertNotEquals(layoutSignature(flat), layoutSignature(steep));
    }

    private static BuildingManifest manifest(int totalBuildings) {
        List<BuildingDefinition> buildings = new ArrayList<>();
        buildings.add(LayoutTestFixtures.townHall());
        buildings.add(LayoutTestFixtures.well());
        buildings.add(LayoutTestFixtures.dock());
        buildings.add(LayoutTestFixtures.mine());
        buildings.add(LayoutTestFixtures.farmhouse());
        buildings.add(LayoutTestFixtures.watchtower());
        while (buildings.size() < totalBuildings) {
            buildings.add(LayoutTestFixtures.house());
        }
        return new BuildingManifest(buildings);
    }

    private static BuildingManifest stoneConstrainedManifest(int constrainedCount) {
        List<BuildingDefinition> buildings = new ArrayList<>();
        buildings.add(LayoutTestFixtures.townHall());
        for (int i = 0; i < constrainedCount; i++) {
            buildings.add(LayoutTestFixtures.building(
                    "settlements:stone_outpost_" + i,
                    Map.of(),
                    TraitSlot.FLAVOR,
                    100 - i,
                    Set.of(ResourceTag.STONE),
                    false,
                    6,
                    8,
                    6,
                    8
            ));
        }
        return new BuildingManifest(buildings);
    }

    private static boolean insideBuildArea(BoundingRegion buildArea, BoundingRegion bounds) {
        return bounds.min().x() >= buildArea.min().x()
                && bounds.max().x() <= buildArea.max().x()
                && bounds.min().z() >= buildArea.min().z()
                && bounds.max().z() <= buildArea.max().z();
    }

    private static boolean insideBuildAreaXZ(BoundingRegion buildArea, BlockPosition position) {
        return position.x() >= buildArea.min().x()
                && position.x() <= buildArea.max().x()
                && position.z() >= buildArea.min().z()
                && position.z() <= buildArea.max().z();
    }

    private static boolean noOverlaps(List<BuildingAssignment> assignments) {
        for (int i = 0; i < assignments.size(); i++) {
            for (int j = i + 1; j < assignments.size(); j++) {
                if (assignments.get(i).plot().bounds().intersects(assignments.get(j).plot().bounds())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean respectsMinimumSpacing(List<BuildingAssignment> assignments, int spacing) {
        // Road-frontage buildings are placed on opposite sides of roads; the road itself acts as
        // the padding between them (per PlacementGrid.occupyRoad logic). Only check pairs where
        // neither building has road frontage, i.e. nuclei and scatter buildings.
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).plot().roadFrontage()) {
                continue;
            }
            BoundingRegion expanded = assignments.get(i).plot().bounds().expandedBy(spacing);
            for (int j = i + 1; j < assignments.size(); j++) {
                if (assignments.get(j).plot().roadFrontage()) {
                    continue;
                }
                if (expanded.intersects(assignments.get(j).plot().bounds())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BuildingAssignment findById(List<BuildingAssignment> assignments, String id) {
        return assignments.stream().filter(assignment -> assignment.building().id().equals(id)).findFirst().orElseThrow();
    }

    private static long countBuildingsWithIdPrefixNearResource(LayoutResult result, String prefix,
                                                               ResourceTag resource) {
        return result.assignments().stream()
                .filter(assignment -> assignment.building().id().startsWith(prefix))
                .filter(assignment -> assignment.plot().localResources().contains(resource))
                .count();
    }

    private static boolean allRoadFrontageAssignmentsTouchRoad(LayoutResult result) {
        return result.assignments().stream()
                .filter(assignment -> assignment.plot().roadFrontage() || assignment.building().id().equals("settlements:town_hall"))
                .allMatch(assignment -> assignment.building().id().equals("settlements:town_hall")
                        || result.roads().stream().anyMatch(road -> roadNearPlot(road, assignment.plot().bounds())));
    }

    private static BlockPosition planningCenter(LayoutResult result) {
        BuildingAssignment townHall = findById(result.assignments(), "settlements:town_hall");
        return townHall.plot().bounds().centerXZ().withY(townHall.plot().targetY());
    }

    private static ZoneTier expectedZone(BlockPosition origin, BuildingAssignment assignment, ScaleTier scaleTier) {
        return LayoutSupport.zoneForDistance(scaleTier, distanceFromOrigin(origin, assignment));
    }

    private static double distanceFromOrigin(BlockPosition origin, BuildingAssignment assignment) {
        return Math.sqrt(LayoutSupport.distanceSquaredXZ(origin, assignment.plot().bounds().centerXZ()));
    }

    private static List<String> layoutSignature(LayoutResult result) {
        List<String> values = new ArrayList<>();
        values.add("anchor:" + key(result.anchor()));
        result.roads().forEach(road -> values.add("road:" + road.type() + ":" + key(road.start()) + ":" + key(road.end())));
        result.assignments().forEach(assignment -> values.add(
                "assignment:" + assignment.building().id()
                        + ":" + assignment.plot().id()
                        + ":" + assignment.plot().zone()
                        + ":" + assignment.plot().bounds().min().x()
                        + ":" + assignment.plot().bounds().min().z()
                        + ":" + assignment.plot().bounds().max().x()
                        + ":" + assignment.plot().bounds().max().z()
                        + ":" + assignment.facing()
                        + ":" + assignment.plot().roadFrontage()
        ));
        return values;
    }

    private static boolean roadNearPlot(RoadSegment road,
                                        BoundingRegion bounds) {
        BoundingRegion expandedRoad = LayoutSupport.roadBounds(road).expandedBy(2);
        return expandedRoad.intersects(bounds);
    }

    private static String key(BlockPosition position) {
        return position.x() + ":" + position.z();
    }

}
