package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.application.settlement.query.BuildingContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

@Getter
public final class BuildingExitEvent extends SettlementRegionEvent {

    private final BuildingContext buildingContext;

    public BuildingExitEvent(@Nonnull ServerPlayer player,
                             @Nonnull BlockPos position,
                             @Nonnull ResourceKey<Level> dimension,
                             @Nonnull SettlementRegionCause cause,
                             @Nonnull BuildingContext buildingContext) {
        super(player, position, dimension, cause);
        this.buildingContext = buildingContext;
    }

}
