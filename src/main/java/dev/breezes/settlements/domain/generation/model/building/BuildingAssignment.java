package dev.breezes.settlements.domain.generation.model.building;

import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.Plot;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;

/**
 * A building assigned to a spatial plot by the layout engine.
 * <p>
 * Template resolution is deferred to the infrastructure layer, which selects a concrete
 * {@code .nbt} file that fits within the plot bounds. See {@link ResolvedTemplate}.
 */
public record BuildingAssignment(
        BuildingDefinition building,
        Plot plot,
        Direction facing,
        TraitId traitSource
) {
}
