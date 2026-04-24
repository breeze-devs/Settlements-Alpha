package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import dev.breezes.settlements.domain.settlement.query.SettlementContext;
import dev.breezes.settlements.domain.settlement.query.SettlementPositionContext;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.BuildingEnterEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.BuildingExitEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.PlayerRegionState;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementEnterEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementExitEvent;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ServerScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class PlayerSettlementTracker {

    private static final int POLL_INTERVAL_TICKS = Ticks.seconds(1).getTicksAsInt();

    private final BuildingRegistry buildingRegistry;
    private final SettlementQueryService settlementQueryService;
    private final Map<UUID, PlayerRegionState> playerStates = new HashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % POLL_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            trackPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        this.playerStates.remove(event.getEntity().getUUID());
    }

    private void trackPlayer(@Nonnull ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos position = player.blockPosition();
        PlayerRegionState previousState = this.playerStates.getOrDefault(player.getUUID(), PlayerRegionState.empty());

        if (previousState.lastCheckedPosition() != null && position.distSqr(previousState.lastCheckedPosition()) < 1) {
            return;
        }

        SettlementPositionContext currentContext = this.settlementQueryService.getContextAt(level, position);
        PlayerRegionState currentState = PlayerRegionState.from(position, currentContext);

        if (previousState.equals(currentState)) {
            this.playerStates.put(player.getUUID(), currentState);
            return;
        }

        // We emit exits before enters so listeners never observe the player in two regions at once during swaps.
        emitExitEvents(player, level, position, previousState, currentState);
        emitEnterEvents(player, position, previousState, currentContext);
        this.playerStates.put(player.getUUID(), currentState);
    }

    private void emitExitEvents(@Nonnull ServerPlayer player,
                                @Nonnull ServerLevel level,
                                @Nonnull BlockPos position,
                                @Nonnull PlayerRegionState previousState,
                                @Nonnull PlayerRegionState currentState) {
        if (hasChanged(previousState.buildingDefinitionId(), currentState.buildingDefinitionId())) {
            resolvePreviousBuildingContext(level, previousState)
                    .ifPresent(context -> NeoForge.EVENT_BUS.post(new BuildingExitEvent(player, position, context)));
        }

        if (hasChanged(previousState.settlementId(), currentState.settlementId())) {
            resolveSettlementContext(level, previousState.settlementId())
                    .ifPresent(context -> NeoForge.EVENT_BUS.post(new SettlementExitEvent(player, position, context.metadata())));
        }
    }

    private static void emitEnterEvents(@Nonnull ServerPlayer player,
                                        @Nonnull BlockPos position,
                                        @Nonnull PlayerRegionState previousState,
                                        @Nonnull SettlementPositionContext currentContext) {
        currentContext.settlement()
                .filter(context -> hasChanged(previousState.settlementId(), context.metadata().settlementId()))
                .ifPresent(context -> NeoForge.EVENT_BUS.post(new SettlementEnterEvent(player, position, context.metadata())));

        currentContext.building()
                .filter(context -> hasChanged(previousState.buildingDefinitionId(), context.buildingDefinitionId()))
                .ifPresent(context -> NeoForge.EVENT_BUS.post(new BuildingEnterEvent(player, position, context)));
    }

    private Optional<SettlementContext> resolveSettlementContext(@Nonnull ServerLevel level,
                                                                 @Nullable String settlementId) {
        if (settlementId == null || settlementId.isBlank()) {
            return Optional.empty();
        }

        return this.settlementQueryService.getSettlementById(level, settlementId);
    }

    private Optional<BuildingContext> resolvePreviousBuildingContext(@Nonnull ServerLevel level,
                                                                     @Nonnull PlayerRegionState previousState) {
        String buildingDefinitionId = previousState.buildingDefinitionId();
        if (buildingDefinitionId == null || buildingDefinitionId.isBlank()) {
            return Optional.empty();
        }

        Optional<SettlementContext> settlementContext = resolveSettlementContext(level, previousState.settlementId());
        return settlementContext.map(context -> new BuildingContext(
                buildingDefinitionId,
                resolveBuildingDisplayName(buildingDefinitionId),
                context
        ));
    }

    private String resolveBuildingDisplayName(@Nonnull String buildingDefinitionId) {
        String displayName = this.buildingRegistry.displayNameFor(buildingDefinitionId);
        if (displayName.equals(buildingDefinitionId) && this.buildingRegistry.byId(buildingDefinitionId).isEmpty()) {
            log.warn("Missing building definition '{}'; using raw id as display name", buildingDefinitionId);
            return displayName;
        }

        return displayName;
    }

    private static boolean hasChanged(@Nullable String previousValue, @Nullable String currentValue) {
        if (previousValue == null) {
            return currentValue != null;
        }
        return !previousValue.equals(currentValue);
    }

}
