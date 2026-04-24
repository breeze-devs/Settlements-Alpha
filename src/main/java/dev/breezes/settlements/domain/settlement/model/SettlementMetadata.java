package dev.breezes.settlements.domain.settlement.model;

import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record SettlementMetadata(
        @Nonnull String settlementId,
        @Nonnull String name,
        @Nonnull String primaryTrait,
        @Nonnull String scaleTier,
        int centerX,
        int centerZ,
        int boundsMinX,
        int boundsMinZ,
        int boundsMaxX,
        int boundsMaxZ,
        int estimatedPopulation,
        float wealthLevel
) {

    public boolean containsPosition(int x, int z) {
        return x >= boundsMinX && x <= boundsMaxX
                && z >= boundsMinZ && z <= boundsMaxZ;
    }

}
