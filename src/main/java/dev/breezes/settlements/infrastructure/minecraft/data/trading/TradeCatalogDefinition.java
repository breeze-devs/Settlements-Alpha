package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.List;

public record TradeCatalogDefinition(
        @Nonnull VillagerProfessionKey profession,
        @Nonnull List<OfferEntry> offers,
        @Nonnull List<DemandEntry> demands
) {
}
