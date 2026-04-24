package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import javax.annotation.Nonnull;

@Getter
public abstract class SettlementRegionEvent extends Event {

    private final ServerPlayer player;
    private final BlockPos position;

    protected SettlementRegionEvent(@Nonnull ServerPlayer player, @Nonnull BlockPos position) {
        this.player = player;
        this.position = position.immutable();
    }

}
