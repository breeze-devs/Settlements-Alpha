package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

@Getter
public final class SettlementEnterEvent extends SettlementRegionEvent {

    private final Settlement settlement;

    public SettlementEnterEvent(@Nonnull ServerPlayer player,
                                @Nonnull BlockPos position,
                                @Nonnull ResourceKey<Level> dimension,
                                @Nonnull SettlementRegionCause cause,
                                @Nonnull Settlement settlement) {
        super(player, position, dimension, cause);
        this.settlement = settlement;
    }

}
