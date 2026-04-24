package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CustomLog
public class ConstrainedPlacer extends BuildingPlacer {

    private static final double ANGLE_STEP = Math.PI / 8.0d;

    @Override
    public PlacementPhaseResult executePlacement(PlacementContext context) {
        List<BuildingAssignment> placed = new ArrayList<>();
        List<BuildingDefinition> remaining = new ArrayList<>();
        int nextPlotId = context.getStartingPlotId();

        for (BuildingDefinition building : context.getBuildings()) {
            Optional<PlacedCandidate> candidateOptional = this.findConstrainedPlacement(building, context);
            if (candidateOptional.isEmpty()) {
                remaining.add(building);
                log.worldgenWarn("Constrained: demoted {} — no valid position", building.id());
                continue;
            }

            PlacedCandidate candidate = candidateOptional.get();
            ZoneTier zone = LayoutSupport.zoneForDistance(
                    context.scaleTier(),
                    Math.sqrt(LayoutSupport.distanceSquaredXZ(context.getPlanningCenter(), candidate.footprint().center()))
            );
            BuildingAssignment assignment = this.createAssignment(
                    nextPlotId++,
                    building,
                    this.resolveTraitSource(building, context),
                    candidate.footprint(),
                    candidate.facing(),
                    zone,
                    true,
                    candidate.localResources()
            );
            placed.add(assignment);
            context.getGrid().occupy(candidate.footprint().bounds());
            log.worldgenStatus("Constrained: placed {} at ({},{},{}) zone={}",
                    building.id(),
                    candidate.footprint().center().x(),
                    candidate.footprint().center().y(),
                    candidate.footprint().center().z(),
                    zone);
        }

        return new PlacementPhaseResult(placed, remaining);
    }

    private Optional<PlacedCandidate> findConstrainedPlacement(BuildingDefinition building, PlacementContext context) {
        int maxRadius = Math.max(context.buildArea().widthX(), context.buildArea().widthZ()) / 2;
        int step = Math.max(context.terrainGrid().sampleInterval(), 4);

        for (int distance = 8; distance <= maxRadius; distance += step) {
            List<PlacedCandidate> validOnRing = new ArrayList<>();
            for (double angle = 0.0d; angle < Math.PI * 2.0d; angle += ANGLE_STEP) {
                int candidateX = context.getAnchor().x() + (int) Math.round(distance * Math.cos(angle));
                int candidateZ = context.getAnchor().z() + (int) Math.round(distance * Math.sin(angle));
                int candidateY = context.terrainGrid().getHeightAtWorld(candidateX, candidateZ);
                BlockPosition center = new BlockPosition(candidateX, candidateY, candidateZ);

                Set<ResourceTag> localResources = context.getResourceScanner().scan(context.terrainGrid(), center, 12);
                if (!LayoutSupport.hasRequiredResources(building, localResources)) {
                    continue;
                }

                Direction facing = LayoutSupport.directionToward(center, context.getPlanningCenter());
                LayoutSupport.CandidateFootprint footprint = LayoutSupport.evaluateFootprint(
                        context.terrainGrid(),
                        center,
                        facing,
                        building.footprint()
                );
                PlacementResult result = context.getValidator().evaluate(building, footprint, localResources);
                if (result.valid()) {
                    validOnRing.add(new PlacedCandidate(footprint, facing, result.localResources()));
                }
            }
            if (!validOnRing.isEmpty()) {
                return this.selectBestCandidateOnRing(validOnRing, context);
            }
        }

        return Optional.empty();
    }

    /**
     * Constrained/resource buildings still search outward from the site anchor, but once a ring
     * produces valid candidates, the winning placement should align with the urban center model.
     * That means selecting the candidate closest to the planning center rather than the original
     * search origin.
     */
    private Optional<PlacedCandidate> selectBestCandidateOnRing(List<PlacedCandidate> validOnRing,
                                                                PlacementContext context) {
        return validOnRing.stream()
                .min(Comparator.comparingLong(candidate ->
                        LayoutSupport.distanceSquaredXZ(context.getPlanningCenter(), candidate.footprint().center())));
    }

    private record PlacedCandidate(
            LayoutSupport.CandidateFootprint footprint,
            Direction facing,
            Set<ResourceTag> localResources
    ) {
    }

}
