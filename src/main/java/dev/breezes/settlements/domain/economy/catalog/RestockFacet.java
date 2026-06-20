package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

@Builder
public record RestockFacet(
        int below,
        int buyPricePerUnit,
        int priority
) {

    public RestockFacet {
        if (below <= 0) {
            throw new IllegalArgumentException("Restock below must be positive");
        }
        if (buyPricePerUnit <= 0) {
            throw new IllegalArgumentException("Restock buyPricePerUnit must be positive");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Restock priority must be non-negative");
        }
    }

}
