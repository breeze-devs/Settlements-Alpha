package dev.breezes.settlements.application.ui.stats.model;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
public record DemandDisplayEntry(
        @Nonnull String id,
        @Nonnull ItemMatch match,
        int desiredMinCount,
        int basePricePerUnit,
        int basePriority,
        @Nullable ActiveDemand activeDemand
) {

    public DemandDisplayEntry {
        if (id.isBlank()) {
            throw new IllegalArgumentException("DemandDisplayEntry id must not be blank");
        }
        if (desiredMinCount <= 0) {
            throw new IllegalArgumentException("DemandDisplayEntry desiredMinCount must be positive");
        }
        if (basePricePerUnit <= 0) {
            throw new IllegalArgumentException("DemandDisplayEntry basePricePerUnit must be positive");
        }
        if (basePriority < 0) {
            throw new IllegalArgumentException("DemandDisplayEntry basePriority must be non-negative");
        }
    }

    public boolean active() {
        return this.activeDemand != null;
    }

}
