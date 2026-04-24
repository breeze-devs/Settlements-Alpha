package dev.breezes.settlements.domain.settlement.query;

import javax.annotation.Nullable;
import java.util.Optional;

public record SettlementPositionContext(
        @Nullable SettlementContext settlementContext,
        @Nullable BuildingContext buildingContext
) {

    public static SettlementPositionContext empty() {
        return new SettlementPositionContext(null, null);
    }

    public boolean hasSettlement() {
        return this.settlementContext != null;
    }

    public boolean hasBuilding() {
        return this.buildingContext != null;
    }

    public Optional<SettlementContext> settlement() {
        return Optional.ofNullable(this.settlementContext);
    }

    public Optional<BuildingContext> building() {
        return Optional.ofNullable(this.buildingContext);
    }

}
