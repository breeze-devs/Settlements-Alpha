package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.query.BuildingContext;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.BuildingEnterEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.BuildingExitEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.PlayerRegionState;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementDisbandedEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementEnterEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementExitEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementFoundedEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementLifecycleEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementRecordedEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementRegionCause;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes settlement and building entry and exit events for server players as their region membership changes.
 * <p>
 * Each transition publishes exits before entries. Building exits precede settlement exits, and settlement entries
 * precede building entries. Logout clears tracking without publishing exits.
 * <p>
 * Chunk changes are evaluated at server-tick end; building changes within a chunk are sampled periodically.
 * Founding and recording queue a recheck for server-tick end. Disbanding publishes exits immediately.
 */
@ServerScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PlayerSettlementTracker {

    /**
     * How often a player inside a settlement is checked again.
     */
    private static final ClockTicks BUILDING_CHECK_INTERVAL = ClockTicks.seconds(1);

    private final SettlementQueryService settlementQueryService;
    private final SettlementStore settlementStore;
    private final BuildingRegistry buildingRegistry;

    /**
     * Last known regions for tracked players; a player outside every settlement has none.
     */
    private final Map<UUID, PlayerRegionState> playerStates = new HashMap<>();

    /**
     * Periodic building-check timers for players tracked in settlements.
     */
    private final Map<UUID, Tickable> buildingCheckCooldowns = new HashMap<>();

    /**
     * Players awaiting a region check at server-tick end, in first-queued order.
     */
    private final Set<UUID> movedPlayers = new LinkedHashSet<>();

    /**
     * Pending settlement rechecks in insertion order, with the entry cause for each settlement.
     */
    private final Map<StoredSettlement, SettlementRegionCause> pendingRechecks = new LinkedHashMap<>();

    @SubscribeEvent
    public void onEnteringSection(EntityEvent.EnteringSection event) {
        // Settlement claims follow chunk boundaries, so vertical section changes cannot change settlement membership.
        if (event.didChunkChange() && event.getEntity() instanceof ServerPlayer player) {
            this.movedPlayers.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        this.placeServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        this.placeServerPlayer(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        this.placeServerPlayer(event.getEntity());
    }

    /**
     * Advances the player's building-check timer, queuing a region check when due.
     */
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Building boundaries can fall within a chunk, so chunk-change events alone cannot detect every crossing.
        Tickable buildingCheck = this.buildingCheckCooldowns.get(player.getUUID());
        if (buildingCheck != null && buildingCheck.tickCheckAndReset(1)) {
            this.movedPlayers.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        this.playerStates.remove(playerId);
        this.buildingCheckCooldowns.remove(playerId);
        this.movedPlayers.remove(playerId);
    }

    @SubscribeEvent
    public void onSettlementFounded(SettlementFoundedEvent event) {
        this.scheduleRecheck(event, SettlementRegionCause.FOUNDED);
    }

    @SubscribeEvent
    public void onSettlementRecorded(SettlementRecordedEvent event) {
        this.scheduleRecheck(event, SettlementRegionCause.RECORDED);
    }

    @SubscribeEvent
    public void onSettlementDisbanded(SettlementDisbandedEvent event) {
        // Publish exits upon disbanding: a later lookup cannot recover the removed settlement needed by exit events
        // Use the event's settlement and cached region state to avoid chunk-loading queries
        Settlement disbanded = event.getSettlement();
        PlayerRegionState inDisbanded = new PlayerRegionState(event.getLevel().dimension(), disbanded.id(), null);
        List<UUID> playersInDisbandedSettlement = this.playerStates.entrySet().stream()
                .filter(held -> held.getValue().isInSameSettlementAs(inDisbanded))
                .map(Map.Entry::getKey)
                .toList();

        MinecraftServer server = event.getLevel().getServer();
        for (UUID playerId : playersInDisbandedSettlement) {
            PlayerRegionState previous = this.playerStates.get(playerId);
            // An earlier player's exit listener may already have moved this player out of the settlement
            if (previous == null || !previous.isInSameSettlementAs(inDisbanded)) {
                continue;
            }

            // Clear tracking before publishing exits so reentrant placement starts outside the disbanded settlement
            this.playerStates.remove(playerId);
            this.buildingCheckCooldowns.remove(playerId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                this.postExitEvents(player, player.blockPosition(), previous, disbanded, true, SettlementRegionCause.DISBANDED);
            }
        }
    }

    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event) {
        // Player ticking can temporarily advance and restore the position. Checking at server-tick end avoids
        // false region transitions at claim borders.
        // Process movement while pendingRechecks still supplies lifecycle entry causes for newly stored settlements.
        // Snapshot and clear before posting events so listener-triggered movement stays queued for the next tick.
        List<UUID> moved = List.copyOf(this.movedPlayers);
        this.movedPlayers.clear();
        for (UUID playerId : moved) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                this.place(player, SettlementRegionCause.MOVED);
            }
        }

        if (this.pendingRechecks.isEmpty()) {
            return;
        }

        // Snapshot and clear before rechecking so settlements stored by listeners stay queued for the next tick.
        Map<StoredSettlement, SettlementRegionCause> due = new LinkedHashMap<>(this.pendingRechecks);
        this.pendingRechecks.clear();
        due.forEach((stored, cause) -> this.recheck(event.getServer(), stored, cause));
    }

    private void scheduleRecheck(SettlementLifecycleEvent event, SettlementRegionCause cause) {
        // Recording can occur during chunk loading. Defer rechecks until the callback returns because building
        // lookups can request chunks and deadlock while the current load is unfinished.
        this.pendingRechecks.put(new StoredSettlement(event.getLevel().dimension(), event.getSettlement().id()), cause);
    }

    /**
     * Reconciles players within the stored settlement's claim. Does nothing if its level or settlement is unavailable.
     */
    private void recheck(MinecraftServer server, StoredSettlement stored, SettlementRegionCause cause) {
        ServerLevel level = server.getLevel(stored.dimension());
        Optional<Settlement> settlement = level == null
                ? Optional.empty()
                : this.settlementStore.find(level, stored.settlementId());
        // A queued settlement may have disbanded before this recheck.
        if (settlement.isEmpty()) {
            return;
        }

        SettlementBounds claim = settlement.get().bounds();
        // Placement listeners may change dimensions, modifying the level's player list during iteration.
        for (ServerPlayer player : List.copyOf(level.players())) {
            ChunkPos chunk = player.chunkPosition();
            if (claim.contains(new ChunkPosition(chunk.x, chunk.z))) {
                this.place(player, cause);
            }
        }
    }

    private void placeServerPlayer(@Nonnull Player player) {
        // Level entry may not produce a section-change event, so reconcile the region immediately
        if (player instanceof ServerPlayer serverPlayer) {
            this.place(serverPlayer, SettlementRegionCause.MOVED);
        }
    }

    /**
     * Updates the player's tracked region from the current position and publishes any resulting transitions.
     *
     * @param cause the transition cause; a pending settlement recheck overrides it for entry events
     */
    private void place(@Nonnull ServerPlayer player, @Nonnull SettlementRegionCause cause) {
        ServerLevel level = player.serverLevel();
        BlockPos position = player.blockPosition();
        Optional<Settlement> settlement = this.settlementQueryService.getSettlementAt(level, position);
        Optional<BuildingContext> building = this.settlementQueryService.getBuildingAt(level, position);
        PlayerRegionState next = settlement
                .map(found -> new PlayerRegionState(level.dimension(), found.id(),
                        building.map(BuildingContext::buildingDefinitionId).orElse(null)))
                .orElse(PlayerRegionState.OUTSIDE);

        UUID playerId = player.getUUID();
        PlayerRegionState previous = this.playerStates.getOrDefault(playerId, PlayerRegionState.OUTSIDE);

        // Update tracking before publishing events so re-entry placement compares against the latest result
        if (next.settlementId() == null) {
            this.playerStates.remove(playerId);
        } else {
            this.playerStates.put(playerId, next);
        }

        if (settlement.filter(found -> found.origin() == SettlementOrigin.GENERATED).isPresent()) {
            this.buildingCheckCooldowns.computeIfAbsent(playerId, id -> Tickable.staggered(BUILDING_CHECK_INTERVAL));
        } else {
            this.buildingCheckCooldowns.remove(playerId);
        }

        boolean settlementChanged = !previous.isInSameSettlementAs(next);
        boolean buildingChanged = !previous.isInSameBuildingAs(next);
        if (buildingChanged && previous.settlementId() != null) {
            // A missing settlement cannot supply the exit event's payload; disbanding handles those exits explicitly
            this.findSettlement(player.server, previous)
                    .ifPresent(previousSettlement -> this.postExitEvents(player, position, previous, previousSettlement, settlementChanged, cause));
        }

        // Movement can trigger placement before a new claim's recheck. Preserve the lifecycle cause so that entry
        // is attributed to the new settlement, even when a movement check detects it first.
        SettlementRegionCause entryCause = settlement
                .map(entered -> this.pendingRechecks.getOrDefault(new StoredSettlement(level.dimension(), entered.id()), cause))
                .orElse(cause);
        if (settlementChanged) {
            settlement.ifPresent(entered ->
                    NeoForge.EVENT_BUS.post(new SettlementEnterEvent(player, position, level.dimension(), entryCause, entered)));
        }

        if (buildingChanged) {
            building.ifPresent(entered ->
                    NeoForge.EVENT_BUS.post(new BuildingEnterEvent(player, position, level.dimension(), entryCause, entered)));
        }
    }

    /**
     * Publishes exit events for the previous region.
     *
     * @param left           the previous region; must identify a settlement
     * @param leftSettlement the settlement identified by left
     * @param settlementLeft whether to publish a settlement exit in addition to any building exit
     */
    private void postExitEvents(ServerPlayer player,
                                BlockPos position,
                                PlayerRegionState left,
                                Settlement leftSettlement,
                                boolean settlementLeft,
                                SettlementRegionCause cause) {
        ResourceKey<Level> dimension = left.dimension();
        String definitionId = left.buildingDefinitionId();
        if (definitionId != null) {
            BuildingContext leftBuilding = new BuildingContext(definitionId,
                    this.buildingRegistry.displayNameFor(definitionId), leftSettlement);
            NeoForge.EVENT_BUS.post(new BuildingExitEvent(player, position, dimension, cause, leftBuilding));
        }
        if (settlementLeft) {
            NeoForge.EVENT_BUS.post(new SettlementExitEvent(player, position, dimension, cause, leftSettlement));
        }
    }

    /**
     * Resolves the region's settlement in its own dimension. Returns empty if the region is outside settlements,
     * its level is unavailable, or its settlement is no longer stored.
     */
    private Optional<Settlement> findSettlement(@Nonnull MinecraftServer server, @Nonnull PlayerRegionState region) {
        if (region.dimension() == null || region.settlementId() == null) {
            return Optional.empty();
        }

        ServerLevel level = server.getLevel(region.dimension());
        return level == null ? Optional.empty() : this.settlementStore.find(level, region.settlementId());
    }

    /**
     * Identifies a settlement within a dimension.
     */
    private record StoredSettlement(ResourceKey<Level> dimension, UUID settlementId) {
    }

}
