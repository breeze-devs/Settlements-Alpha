package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record DemandEntry(
        @Nonnull String id,
        @Nonnull ItemMatch match,
        int desiredMinCount,
        int basePricePerUnit,
        int basePriority
) {

    public DemandEntry {
        if (id.isBlank()) {
            throw new IllegalArgumentException("DemandEntry id must not be blank");
        }
        if (desiredMinCount <= 0) {
            throw new IllegalArgumentException("DemandEntry desiredMinCount must be positive");
        }
        if (basePricePerUnit <= 0) {
            throw new IllegalArgumentException("DemandEntry basePricePerUnit must be positive");
        }
        if (basePriority < 0) {
            throw new IllegalArgumentException("DemandEntry basePriority must be non-negative");
        }
    }

}
