package dev.breezes.settlements.application.ui.stats.model;

import dev.breezes.settlements.domain.economy.catalog.OfferEntry;

import javax.annotation.Nonnull;
import java.util.List;

public record VillagerTradeCatalogSnapshot(
        @Nonnull List<OfferEntry> offers
) {

    public VillagerTradeCatalogSnapshot {
        offers = List.copyOf(offers);
    }

}
