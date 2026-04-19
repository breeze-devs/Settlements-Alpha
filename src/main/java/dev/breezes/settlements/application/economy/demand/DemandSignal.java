package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
public record DemandSignal(
        @Nonnull ItemMatch match,
        int desiredCount,
        int priorityBoost,
        @Nullable Integer pricePerUnitOverride,
        @Nonnull String source,
        long createdGameTime,
        long lastTouchedGameTime
) {

    public DemandSignal {
        if (desiredCount <= 0) {
            throw new IllegalArgumentException("DemandSignal desiredCount must be positive");
        }
        if (priorityBoost < 0) {
            throw new IllegalArgumentException("DemandSignal priorityBoost must be non-negative");
        }
        if (pricePerUnitOverride != null && pricePerUnitOverride <= 0) {
            throw new IllegalArgumentException("DemandSignal pricePerUnitOverride must be positive when present");
        }
        if (source.isBlank()) {
            throw new IllegalArgumentException("DemandSignal source must not be blank");
        }
    }

}
