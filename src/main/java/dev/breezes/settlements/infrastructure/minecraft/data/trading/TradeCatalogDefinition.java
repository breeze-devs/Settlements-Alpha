package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import dev.breezes.settlements.domain.economy.catalog.StockPolicy;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import javax.annotation.Nonnull;
import java.util.List;

public record TradeCatalogDefinition(
        @Nonnull VillagerProfessionKey profession,
        @Nonnull List<StockPolicy> stockPolicies
) {
}
