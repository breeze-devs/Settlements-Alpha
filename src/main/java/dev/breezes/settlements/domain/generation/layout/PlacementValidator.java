package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.shared.logging.levels.LogLevel;
import lombok.CustomLog;

import javax.annotation.Nullable;
import java.util.Set;

@CustomLog(topic = "worldgen")
public class PlacementValidator {

    private final BoundingRegion buildArea;
    private final PlacementGrid grid;
    private final TerrainGrid terrainGrid;
    private final LocalResourceScanner resourceScanner;

    public PlacementValidator(BoundingRegion buildArea,
                              PlacementGrid grid,
                              TerrainGrid terrainGrid,
                              LocalResourceScanner resourceScanner) {
        this.buildArea = buildArea;
        this.grid = grid;
        this.terrainGrid = terrainGrid;
        this.resourceScanner = resourceScanner;
    }

    public PlacementResult evaluate(BuildingDefinition building, LayoutSupport.CandidateFootprint footprint) {
        return this.evaluate(building, footprint, null);
    }

    public PlacementResult evaluate(BuildingDefinition building,
                                    LayoutSupport.CandidateFootprint footprint,
                                    @Nullable Set<ResourceTag> preScannedResources) {
        PlacementResult result;
        if (!LayoutSupport.isWithinBuildAreaXZ(this.buildArea, footprint.bounds())) {
            result = PlacementResult.rejected(PlacementResult.RejectionReason.OUTSIDE_BUILD_AREA);
        } else if (footprint.elevationDelta() > LayoutSupport.FLATNESS_THRESHOLD) {
            result = PlacementResult.rejected(PlacementResult.RejectionReason.TOO_STEEP);
        } else if (this.grid.hasConflict(footprint.bounds())) {
            result = PlacementResult.rejected(PlacementResult.RejectionReason.GRID_CONFLICT);
        } else {
            Set<ResourceTag> resources = preScannedResources != null
                    ? preScannedResources
                    : this.resourceScanner.scan(this.terrainGrid, footprint.center(), resourceScanRadius(building));

            if (!LayoutSupport.hasRequiredResources(building, resources)) {
                result = PlacementResult.rejected(PlacementResult.RejectionReason.MISSING_REQUIRED_RESOURCE);
            } else if (!LayoutSupport.hasNoForbiddenResources(building, resources)) {
                result = PlacementResult.rejected(PlacementResult.RejectionReason.FORBIDDEN_RESOURCE_PRESENT);
            } else if (LayoutSupport.isOnWater(this.terrainGrid, this.resourceScanner, footprint, LayoutSupport.allowsPartialWater(building))) {
                result = PlacementResult.rejected(PlacementResult.RejectionReason.ON_WATER);
            } else {
                result = PlacementResult.accepted(resources);
            }
        }

        // Runs once per placement candidate, so the guard keeps the varargs array and the boxed coordinates off the
        // generation path while TRACE is off
        if (log.isEnabled(LogLevel.TRACE)) {
            log.trace("Placement validation: building={} center=({},{},{}) result={}",
                    building.id(),
                    footprint.center().x(),
                    footprint.center().y(),
                    footprint.center().z(),
                    result.valid() ? "ACCEPTED" : result.rejection());
        }
        return result;
    }

    private static int resourceScanRadius(BuildingDefinition building) {
        return building.requiresResources().isEmpty() ? 8 : 12;
    }

}
