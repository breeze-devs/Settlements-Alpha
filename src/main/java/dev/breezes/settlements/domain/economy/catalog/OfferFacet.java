package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

@Builder
public record OfferFacet(
        int above,
        int basePrice,
        int priceJitter,
        int bundleSize
) {

    public OfferFacet {
        if (above < 0) {
            throw new IllegalArgumentException("Offer above must be non-negative");
        }
        if (basePrice <= 0) {
            throw new IllegalArgumentException("Offer basePrice must be positive");
        }
        if (priceJitter < 0) {
            throw new IllegalArgumentException("Offer priceJitter must be non-negative");
        }
        if (bundleSize <= 0) {
            throw new IllegalArgumentException("Offer bundleSize must be positive");
        }
    }

}
