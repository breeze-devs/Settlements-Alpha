package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

import javax.annotation.Nonnull;

/**
 * Read-only projection of a {@link StockPolicy} dump facet. Value invariants are enforced
 * upstream by {@link DumpFacet}, so this carrier performs no validation of its own.
 */
@Builder
public record SupplyEntry(
        @Nonnull String id,
        @Nonnull ItemMatch match,
        int dumpAbove
) {
}
