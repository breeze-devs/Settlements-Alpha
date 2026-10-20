package dev.breezes.settlements.application.settlement.query;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Answers which settlement, and which building of a generated settlement, holds a position in a level.
 */
public interface SettlementQueryService {

    /**
     * Returns the settlement claiming that claims the chunk at position.
     */
    Optional<Settlement> getSettlementAt(@Nonnull ServerLevel level, @Nonnull BlockPos position);

    /**
     * Returns the settlement building that claims the chunk at position.
     */
    Optional<BuildingContext> getBuildingAt(@Nonnull ServerLevel level, @Nonnull BlockPos position);

}
