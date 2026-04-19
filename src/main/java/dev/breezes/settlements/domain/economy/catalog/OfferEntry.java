package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record OfferEntry(
        @Nonnull String id,
        @Nonnull ItemMatch match,
        int bundleSize,
        int basePrice,
        int priceJitter,
        int surplusThreshold
) {

    public OfferEntry {
        if (id.isBlank()) {
            throw new IllegalArgumentException("Offer id must not be blank");
        }
        if (bundleSize <= 0) {
            throw new IllegalArgumentException("Offer bundleSize must be positive");
        }
        if (basePrice <= 0) {
            throw new IllegalArgumentException("Offer basePrice must be positive");
        }
        if (priceJitter < 0) {
            throw new IllegalArgumentException("Offer priceJitter must be non-negative");
        }
        if (surplusThreshold < 0) {
            throw new IllegalArgumentException("Offer surplusThreshold must be non-negative");
        }
    }

}
