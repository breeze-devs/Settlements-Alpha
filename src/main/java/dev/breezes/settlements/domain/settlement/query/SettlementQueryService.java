package dev.breezes.settlements.domain.settlement.query;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface SettlementQueryService {

    SettlementPositionContext getContextAt(@Nonnull ServerLevel level, @Nonnull BlockPos position);

    Optional<SettlementContext> getSettlementById(@Nonnull ServerLevel level, @Nonnull String settlementId);

    Optional<SettlementContext> getSettlementAt(@Nonnull ServerLevel level, @Nonnull BlockPos position);

    Optional<BuildingContext> getBuildingAt(@Nonnull ServerLevel level, @Nonnull BlockPos position);

}
