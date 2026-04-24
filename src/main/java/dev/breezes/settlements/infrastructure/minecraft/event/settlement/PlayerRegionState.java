package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import dev.breezes.settlements.domain.settlement.query.SettlementPositionContext;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record PlayerRegionState(
        @Nullable BlockPos lastCheckedPosition,
        @Nullable String settlementId,
        @Nullable String buildingDefinitionId
) {

    public static PlayerRegionState empty() {
        return new PlayerRegionState(null, null, null);
    }

    public static PlayerRegionState from(@Nonnull BlockPos position, @Nonnull SettlementPositionContext context) {
        return new PlayerRegionState(
                position.immutable(),
                context.settlement().map(settlement -> settlement.metadata().settlementId()).orElse(null),
                context.building().map(BuildingContext::buildingDefinitionId).orElse(null)
        );
    }

}
