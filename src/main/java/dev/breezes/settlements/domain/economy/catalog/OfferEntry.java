package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

/**
 * Read-only projection of a {@link StockPolicy} offer facet. Value invariants are enforced
 * upstream by {@link OfferFacet}, so this carrier performs no validation of its own.
 */
@Builder
public record OfferEntry(
        String id,
        ItemMatch match,
        int bundleSize,
        int basePrice,
        int priceJitter,
        int surplusThreshold
) {
}
