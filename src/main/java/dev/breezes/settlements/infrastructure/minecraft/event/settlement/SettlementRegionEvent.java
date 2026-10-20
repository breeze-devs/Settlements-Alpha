package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import javax.annotation.Nonnull;

@Getter
public abstract class SettlementRegionEvent extends Event {

    private final ServerPlayer player;
    private final BlockPos position;

    /**
     * The dimension storing the region's settlement. An exit posted after a dimension change names the dimension the
     * player left, not the one the player is in.
     */
    private final ResourceKey<Level> dimension;

    private final SettlementRegionCause cause;

    protected SettlementRegionEvent(@Nonnull ServerPlayer player,
                                    @Nonnull BlockPos position,
                                    @Nonnull ResourceKey<Level> dimension,
                                    @Nonnull SettlementRegionCause cause) {
        this.player = player;
        this.position = position.immutable();
        this.dimension = dimension;
        this.cause = cause;
    }

}
