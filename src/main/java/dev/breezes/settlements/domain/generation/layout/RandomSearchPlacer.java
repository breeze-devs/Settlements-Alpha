package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@CustomLog
public class RandomSearchPlacer extends BuildingPlacer {

    private final CandidateGenerator candidateGenerator;
    private final int maxAttempts;
    private final boolean roadFrontage;

    private RandomSearchPlacer(CandidateGenerator candidateGenerator, int maxAttempts, boolean roadFrontage) {
        this.candidateGenerator = candidateGenerator;
        this.maxAttempts = maxAttempts;
        this.roadFrontage = roadFrontage;
    }

    public static RandomSearchPlacer radial(int minDistance, int maxDistance, int maxAttempts) {
        return new RandomSearchPlacer((anchor, buildArea, terrainGrid, random) -> {
            double angle = random.nextDouble(Math.PI * 2.0d);
            int distance = random.nextInt(minDistance, maxDistance + 1);
            int x = anchor.x() + (int) Math.round(distance * Math.cos(angle));
            int z = anchor.z() + (int) Math.round(distance * Math.sin(angle));
            int y = terrainGrid.getHeightAtWorld(x, z);
            return new BlockPosition(x, y, z);
        }, maxAttempts, true);
    }

    public static RandomSearchPlacer uniform(int maxAttempts) {
        return new RandomSearchPlacer((anchor, buildArea, terrainGrid, random) -> {
            int x = random.nextInt(buildArea.min().x(), buildArea.max().x() + 1);
            int z = random.nextInt(buildArea.min().z(), buildArea.max().z() + 1);
            int y = terrainGrid.getHeightAtWorld(x, z);
            return new BlockPosition(x, y, z);
        }, maxAttempts, false);
    }

    @Override
    public PlacementPhaseResult executePlacement(PlacementContext context) {
        List<BuildingAssignment> placed = new ArrayList<>();
        List<BuildingDefinition> remaining = new ArrayList<>();
        int nextPlotId = context.getStartingPlotId();

        for (BuildingDefinition building : context.getBuildings()) {
            boolean wasPlaced = false;
            for (int attempt = 0; attempt < this.maxAttempts; attempt++) {
                BlockPosition center = this.candidateGenerator.generate(
                        context.getAnchor(), context.buildArea(), context.terrainGrid(), context.getRandom()
                );
                Direction facing = LayoutSupport.directionToward(center, context.getPlanningCenter());
                LayoutSupport.CandidateFootprint footprint = LayoutSupport.evaluateFootprint(
                        context.terrainGrid(),
                        center,
                        facing,
                        building.footprint()
                );
                PlacementResult result = context.getValidator().evaluate(building, footprint);
                if (!result.valid()) {
                    continue;
                }

                ZoneTier zone = LayoutSupport.zoneForDistance(
                        context.scaleTier(),
                        Math.sqrt(LayoutSupport.distanceSquaredXZ(context.getPlanningCenter(), center))
                );
                BuildingAssignment assignment = this.createAssignment(
                        nextPlotId++,
                        building,
                        this.resolveTraitSource(building, context),
                        footprint,
                        facing,
                        zone,
                        this.roadFrontage,
                        result.localResources()
                );
                placed.add(assignment);
                context.getGrid().occupy(footprint.bounds());
                wasPlaced = true;
                log.worldgenStatus("RandomSearch: placed {} at ({},{},{}) zone={}",
                        building.id(), center.x(), center.y(), center.z(), zone);
                break;
            }

            if (!wasPlaced) {
                remaining.add(building);
                log.worldgenWarn("RandomSearch: failed to place {} after {} attempts", building.id(), this.maxAttempts);
            }
        }

        return new PlacementPhaseResult(placed, remaining);
    }

    @FunctionalInterface
    private interface CandidateGenerator {
        BlockPosition generate(BlockPosition anchor, BoundingRegion buildArea, TerrainGrid terrainGrid, Random random);
    }

}
