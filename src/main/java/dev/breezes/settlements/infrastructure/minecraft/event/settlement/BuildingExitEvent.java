package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

@Getter
public final class BuildingExitEvent extends SettlementRegionEvent {

    private final BuildingContext buildingContext;

    public BuildingExitEvent(@Nonnull ServerPlayer player,
                             @Nonnull BlockPos position,
                             @Nonnull BuildingContext buildingContext) {
        super(player, position);
        this.buildingContext = buildingContext;
    }

}
