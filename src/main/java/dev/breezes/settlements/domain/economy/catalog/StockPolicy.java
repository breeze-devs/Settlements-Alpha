package dev.breezes.settlements.domain.economy.catalog;

import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
public record StockPolicy(
        @Nonnull String id,
        @Nonnull ItemMatch match,
        @Nullable RestockFacet restock,
        @Nullable OfferFacet offer,
        @Nullable DumpFacet dump
) {

    public StockPolicy {
        if (id.isBlank()) {
            throw new IllegalArgumentException("StockPolicy id must not be blank");
        }
        if (restock == null && offer == null && dump == null) {
            throw new IllegalArgumentException("StockPolicy must define at least one stock facet");
        }

        Integer previousRung = null;
        if (restock != null) {
            previousRung = restock.below();
        }
        if (offer != null) {
            validateOrderedRung(previousRung, offer.above(), "offer.above");
            previousRung = offer.above();
        }
        if (dump != null) {
            validateOrderedRung(previousRung, dump.above(), "dump.above");
        }
    }

    @Nonnull
    public DemandEntry toDemandEntry() {
        if (this.restock == null) {
            throw new IllegalStateException("StockPolicy has no restock facet");
        }

        return DemandEntry.builder()
                .id(this.id)
                .match(this.match)
                .desiredMinCount(this.restock.below())
                .basePricePerUnit(this.restock.buyPricePerUnit())
                .basePriority(this.restock.priority())
                .build();
    }

    @Nonnull
    public OfferEntry toOfferEntry() {
        if (this.offer == null) {
            throw new IllegalStateException("StockPolicy has no offer facet");
        }

        return OfferEntry.builder()
                .id(this.id)
                .match(this.match)
                .bundleSize(this.offer.bundleSize())
                .basePrice(this.offer.basePrice())
                .priceJitter(this.offer.priceJitter())
                .surplusThreshold(this.offer.above())
                .build();
    }

    @Nonnull
    public SupplyEntry toSupplyEntry() {
        if (this.dump == null) {
            throw new IllegalStateException("StockPolicy has no dump facet");
        }

        return SupplyEntry.builder()
                .id(this.id)
                .match(this.match)
                .dumpAbove(this.dump.above())
                .build();
    }

    private static void validateOrderedRung(@Nullable Integer previousRung, int currentRung, @Nonnull String fieldName) {
        if (previousRung != null && previousRung > currentRung) {
            throw new IllegalArgumentException("StockPolicy rung order violated at " + fieldName);
        }
    }

}
