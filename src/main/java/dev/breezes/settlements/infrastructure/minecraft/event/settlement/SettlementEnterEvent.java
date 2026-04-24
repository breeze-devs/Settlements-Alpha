package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

@Getter
public final class SettlementEnterEvent extends SettlementRegionEvent {

    private final SettlementMetadata settlementMetadata;

    public SettlementEnterEvent(@Nonnull ServerPlayer player,
                                @Nonnull BlockPos position,
                                @Nonnull SettlementMetadata settlementMetadata) {
        super(player, position);
        this.settlementMetadata = settlementMetadata;
    }

}
