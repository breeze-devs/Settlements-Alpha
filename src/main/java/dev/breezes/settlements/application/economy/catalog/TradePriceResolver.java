package dev.breezes.settlements.application.economy.catalog;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TradePriceResolver {

    @Nonnull
    private final TradeCatalogRegistry tradeCatalogRegistry;

    public Optional<Integer> resolveDemandPrice(@Nonnull BaseVillager buyer, @Nonnull ItemMatch want) {
        VillagerProfessionKey profession = buyer.getProfession();
        return this.resolveDemandPrice(profession, want);
    }

    public Optional<Integer> resolveOfferBundlePrice(@Nonnull BaseVillager seller, @Nonnull ItemMatch want) {
        VillagerProfessionKey profession = seller.getProfession();
        return this.resolveOfferBundlePrice(profession, want);
    }

    public Optional<Integer> resolveDemandPrice(@Nonnull VillagerProfessionKey profession, @Nonnull ItemMatch want) {
        // Price resolution uses the highest-priority (most specific) match only.
        return this.tradeCatalogRegistry.findDemands(profession, want).stream()
                .findFirst()
                .map(this::resolveDemandEntryPrice);
    }

    public Optional<Integer> resolveOfferBundlePrice(@Nonnull VillagerProfessionKey profession, @Nonnull ItemMatch want) {
        // Price resolution uses the highest-priority (most specific) match only.
        return this.tradeCatalogRegistry.findOffers(profession, want).stream()
                .findFirst()
                .map(this::resolveOfferEntryBundlePrice);
    }

    private int resolveDemandEntryPrice(@Nonnull DemandEntry entry) {
        return entry.basePricePerUnit();
    }

    private int resolveOfferEntryBundlePrice(@Nonnull OfferEntry entry) {
        return Math.max(1, entry.basePrice() + jitter(entry.priceJitter()));
    }

    private static int jitter(int priceJitter) {
        if (priceJitter == 0) {
            return 0;
        }
        return RandomUtil.randomInt(-priceJitter, priceJitter, true);
    }

}
