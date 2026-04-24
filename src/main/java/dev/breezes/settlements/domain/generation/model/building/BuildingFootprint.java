package dev.breezes.settlements.domain.generation.model.building;

import lombok.Builder;

@Builder
public record BuildingFootprint(
        int width,
        int depth
) {

    public BuildingFootprint {
        if (width <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Footprint dimensions must be > 0");
        }
    }

    public boolean fits(int actualWidth, int actualDepth) {
        return actualWidth <= this.width && actualDepth <= this.depth;
    }

}
