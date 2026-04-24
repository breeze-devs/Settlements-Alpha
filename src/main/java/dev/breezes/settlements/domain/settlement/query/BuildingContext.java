package dev.breezes.settlements.domain.settlement.query;

import javax.annotation.Nonnull;

public record BuildingContext(
        @Nonnull String buildingDefinitionId,
        @Nonnull String displayName,
        @Nonnull SettlementContext settlementContext
) {
}
