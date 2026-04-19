package dev.breezes.settlements.domain.economy.catalog;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.List;

public interface TradeCatalogRegistry {

    /**
     * Returns all sellable entries for the given seller's profession that could potentially satisfy
     * the buyer's want. Mixed-kind pairs (ItemRef vs. Tag want, or TagRef vs. Item want) are included
     * as candidates and sorted by specificity -- exact-kind matches first.
     *
     * @param sellerProfession the profession of the villager who would be selling
     * @param buyerWant        what the prospective buyer is looking for
     */
    List<OfferEntry> findOffers(@Nonnull VillagerProfessionKey sellerProfession, @Nonnull ItemMatch want);

    /**
     * Returns all buyable entries for the given buyer's profession that could potentially match
     * the specified want, sorted by specificity.
     *
     * @param buyerProfession the profession of the villager who would be buying
     * @param buyerWant       what the buyer wants to acquire
     */
    List<DemandEntry> findDemands(@Nonnull VillagerProfessionKey buyerProfession, @Nonnull ItemMatch want);

    /**
     * Returns all sellable entries registered for the given profession, with no want-key filtering.
     * Prefer {@link #findOffers} when you have a concrete buyer want. Use this method when you
     * need the full unfiltered catalog for a profession (e.g., display or bulk validation).
     */
    List<OfferEntry> offersFor(@Nonnull VillagerProfessionKey profession);

    /**
     * Returns all buyable entries registered for the given profession, with no want-key filtering.
     * Prefer {@link #findDemands} when you have a concrete want. Use this method when the caller
     * needs the full unfiltered catalog for presentation or validation.
     */
    List<DemandEntry> demandsFor(@Nonnull VillagerProfessionKey profession);

    /**
     * Monotonic version of the currently loaded catalog snapshot.
     * Callers can cache the last observed value and only rebuild derived views when the data pack
     * reload pipeline has published a new catalog.
     */
    int catalogVersion();

}
