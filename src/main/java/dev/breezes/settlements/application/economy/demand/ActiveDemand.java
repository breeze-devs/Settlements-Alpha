package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record ActiveDemand(
        @Nonnull ItemMatch match,
        int desiredCount,
        int priority,
        int basePricePerUnit,
        @Nonnull Origin origin
) {

    public ActiveDemand {
        if (desiredCount <= 0) {
            throw new IllegalArgumentException("ActiveDemand desiredCount must be positive");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("ActiveDemand priority must be non-negative");
        }
        if (basePricePerUnit <= 0) {
            throw new IllegalArgumentException("ActiveDemand basePricePerUnit must be positive");
        }
    }

    public enum Origin {
        STATIC_SHORTFALL,

        SIGNAL,
        
        STATIC_BOOSTED_BY_SIGNAL,
    }

}
