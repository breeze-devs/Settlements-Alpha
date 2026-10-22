package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

/**
 * Read-only projection of a {@link StockPolicy} restock facet. Value invariants are enforced
 * upstream by {@link RestockFacet}, so this carrier performs no validation of its own.
 */
@Builder
public record DemandEntry(
        String id,
        ItemMatch match,
        int desiredMinCount,
        int basePricePerUnit,
        int basePriority
) {
}
