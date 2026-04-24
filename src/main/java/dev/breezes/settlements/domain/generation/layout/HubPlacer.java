package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import lombok.CustomLog;

import java.util.List;

@CustomLog
public class HubPlacer extends BuildingPlacer {

    private static final int DEFAULT_MAX_ATTEMPTS = 8;
    private static final int DEFAULT_MAX_RADIUS = 48;
    private static final String TOWN_HALL_ID = "settlements:building_definitions/town_hall";

    private final int maxAttempts;
    private final int maxRadius;

    public HubPlacer() {
        this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
        this.maxRadius = DEFAULT_MAX_RADIUS;
    }

    @Override
    public PlacementPhaseResult executePlacement(PlacementContext context) {
        if (context.getBuildings().isEmpty()) {
            return new PlacementPhaseResult(List.of(), List.of());
        }

        BuildingDefinition building = context.getBuildings().getFirst();
        if (!TOWN_HALL_ID.equals(building.id())) {
            throw new IllegalStateException("HubPlacer requires settlements:building_definitions/town_hall, got: " + building.id());
        }

        for (int attempt = 1; attempt <= this.maxAttempts; attempt++) {
            BlockPosition center = this.generatePlacementPosition(context, attempt);
            Direction facing = LayoutSupport.directionToward(center, context.getAnchor());
            LayoutSupport.CandidateFootprint footprint = LayoutSupport.evaluateFootprint(context.terrainGrid(), center, facing, building.footprint());
            PlacementResult result = context.getValidator().evaluate(building, footprint);
            if (result.valid()) {
                BuildingAssignment assignment = this.createAssignment(context.getStartingPlotId(), building, null,
                        footprint, facing, ZoneTier.CORE, true, result.localResources());
                context.getGrid().occupy(footprint.bounds());
                log.worldgenStatus("Hub: placed {} at ({}, {}, {}) on attempt {}", building.id(), center.x(), center.y(), center.z(), attempt);
                return new PlacementPhaseResult(List.of(assignment), List.of());
            }
            log.worldgenWarn("Hub: attempt {} failed for {} at ({}, {}, {}) near anchor: {}",
                    attempt, building.id(), center.x(), center.y(), center.z(), result.rejection());
        }

        log.worldgenError("Hub: failed to place mandatory {} near anchor after {} attempts", building.id(), this.maxAttempts);
        throw new IllegalStateException("Unable to place mandatory town hall near settlement anchor");
    }

    private BlockPosition generatePlacementPosition(PlacementContext context, int attempt) {
        // First attempt is at the anchor
        if (attempt == 1) {
            return context.getAnchor();
        }

        // Subsequent attempts are at random positions around the anchor
        double angle = context.getRandom().nextDouble(Math.PI * 2.0d);
        int distance = context.getRandom().nextInt(0, this.maxRadius + 1);
        int x = context.getAnchor().x() + (int) Math.round(distance * Math.cos(angle));
        int z = context.getAnchor().z() + (int) Math.round(distance * Math.sin(angle));
        int y = context.terrainGrid().getHeightAtWorld(x, z);
        return new BlockPosition(x, y, z);
    }

}
