package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;

import java.util.List;

public record LayoutResult(
        BlockPosition anchor,
        BlockPosition planningCenter,
        List<RoadSegment> roads,
        List<BuildingAssignment> assignments
) {

    public LayoutResult {
        roads = List.copyOf(roads);
        assignments = List.copyOf(assignments);
    }

}
