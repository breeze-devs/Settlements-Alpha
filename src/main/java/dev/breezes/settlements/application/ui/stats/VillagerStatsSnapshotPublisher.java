package dev.breezes.settlements.application.ui.stats;

import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.application.ui.stats.snapshot.VillagerStatsSnapshotBuilder;
import dev.breezes.settlements.application.ui.sync.UiSnapshotPublisher;
import dev.breezes.settlements.application.ui.sync.session.UiSession;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerStatsSnapshotPublisher implements UiSnapshotPublisher {

    private static final int STATS_SNAPSHOT_INTERVAL_TICKS = ClockTicks.of(10).getTicksAsInt();
    private static final int INVENTORY_DIRTY_CHECK_INTERVAL_TICKS = ClockTicks.of(60).getTicksAsInt();
    private static final int CATALOG_DIRTY_CHECK_INTERVAL_TICKS = ClockTicks.of(60).getTicksAsInt();
    private static final int DEMAND_DIRTY_CHECK_INTERVAL_TICKS = ClockTicks.of(60).getTicksAsInt();

    private final VillagerStatsSnapshotBuilder snapshotBuilder;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DemandSignalService demandSignalService;
    private final ConcurrentMap<Long, StatsPublishState> stateBySession = new ConcurrentHashMap<>();

    @Override
    public void onSessionOpened(@Nonnull UiSession session,
                                @Nonnull ServerPlayer player,
                                @Nonnull BaseVillager villager,
                                long gameTime,
                                long dayTime) {
        StatsPublishState state = new StatsPublishState();
        this.stateBySession.put(session.getSessionId(), state);
        this.publishStats(session, player, villager, gameTime, state);
        this.publishInventory(session, player, villager, gameTime, state);
        this.publishTradeCatalog(session, player, villager, gameTime, state);
        this.publishDemand(session, player, villager, gameTime, state);
    }

    @Override
    public void tick(@Nonnull UiSession session,
                     @Nonnull ServerPlayer player,
                     @Nonnull BaseVillager villager,
                     long gameTime,
                     long dayTime) {
        StatsPublishState state = this.stateBySession.computeIfAbsent(session.getSessionId(), ignored -> new StatsPublishState());
        if (gameTime - state.lastStatsSentGameTime >= STATS_SNAPSHOT_INTERVAL_TICKS) {
            this.publishStats(session, player, villager, gameTime, state);
        }
        if (gameTime - state.lastInventoryDirtyCheckGameTime >= INVENTORY_DIRTY_CHECK_INTERVAL_TICKS) {
            state.lastInventoryDirtyCheckGameTime = gameTime;
            this.publishInventoryIfDirty(session, player, villager, gameTime, state);
        }
        if (gameTime - state.lastCatalogDirtyCheckGameTime >= CATALOG_DIRTY_CHECK_INTERVAL_TICKS) {
            state.lastCatalogDirtyCheckGameTime = gameTime;
            this.publishTradeCatalogIfDirty(session, player, villager, gameTime, state);
        }
        if (gameTime - state.lastDemandDirtyCheckGameTime >= DEMAND_DIRTY_CHECK_INTERVAL_TICKS) {
            state.lastDemandDirtyCheckGameTime = gameTime;
            this.publishDemandIfDirty(session, player, villager, gameTime, state);
        }
    }

    @Override
    public void onSessionClosed(@Nonnull UiSession session) {
        this.stateBySession.remove(session.getSessionId());
    }

    private void publishInventoryIfDirty(@Nonnull UiSession session,
                                         @Nonnull ServerPlayer player,
                                         @Nonnull BaseVillager villager,
                                         long gameTime,
                                         @Nonnull StatsPublishState state) {
        int inventoryVersion = villager.getSettlementsInventory().getInventoryVersion();
        if (inventoryVersion == state.lastSentInventoryVersion) {
            return;
        }
        this.publishInventory(session, player, villager, gameTime, state);
    }

    private void publishTradeCatalogIfDirty(@Nonnull UiSession session,
                                            @Nonnull ServerPlayer player,
                                            @Nonnull BaseVillager villager,
                                            long gameTime,
                                            @Nonnull StatsPublishState state) {
        VillagerProfessionKey professionKey = resolveProfessionKey(villager);
        int catalogVersion = this.tradeCatalogRegistry.catalogVersion();
        if (catalogVersion == state.lastSentCatalogVersion && professionKey.equals(state.lastSentCatalogProfessionKey)) {
            return;
        }
        this.publishTradeCatalog(session, player, villager, gameTime, state);
    }

    private void publishDemandIfDirty(@Nonnull UiSession session,
                                      @Nonnull ServerPlayer player,
                                      @Nonnull BaseVillager villager,
                                      long gameTime,
                                      @Nonnull StatsPublishState state) {
        int demandVersion = demandDisplayVersion(villager, this.demandSignalService);
        if (demandVersion == state.lastSentDemandVersion) {
            return;
        }
        this.publishDemand(session, player, villager, gameTime, state);
    }

    private void publishStats(@Nonnull UiSession session,
                              @Nonnull ServerPlayer player,
                              @Nonnull BaseVillager villager,
                              long gameTime,
                              @Nonnull StatsPublishState state) {
        VillagerStatsSnapshot snapshot = this.snapshotBuilder.buildStats(villager, gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerStatsSnapshotPacket(session.getSessionId(), snapshot));
        state.lastStatsSentGameTime = gameTime;
        session.markSnapshotSent(gameTime);
    }

    private void publishInventory(@Nonnull UiSession session,
                                  @Nonnull ServerPlayer player,
                                  @Nonnull BaseVillager villager,
                                  long gameTime,
                                  @Nonnull StatsPublishState state) {
        int inventoryVersion = villager.getSettlementsInventory().getInventoryVersion();
        VillagerInventorySnapshot snapshot = this.snapshotBuilder.buildInventory(villager);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerInventorySnapshotPacket(session.getSessionId(), snapshot));
        state.lastInventoryDirtyCheckGameTime = gameTime;
        state.lastSentInventoryVersion = inventoryVersion;
        session.markSnapshotSent(gameTime);
    }

    private void publishTradeCatalog(@Nonnull UiSession session,
                                     @Nonnull ServerPlayer player,
                                     @Nonnull BaseVillager villager,
                                     long gameTime,
                                     @Nonnull StatsPublishState state) {
        VillagerProfessionKey professionKey = resolveProfessionKey(villager);
        int catalogVersion = this.tradeCatalogRegistry.catalogVersion();
        VillagerTradeCatalogSnapshot snapshot = this.snapshotBuilder.buildTradeCatalog(villager);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerTradeCatalogSnapshotPacket(session.getSessionId(), snapshot));
        state.lastCatalogDirtyCheckGameTime = gameTime;
        state.lastSentCatalogVersion = catalogVersion;
        state.lastSentCatalogProfessionKey = professionKey;
        session.markSnapshotSent(gameTime);
    }

    private void publishDemand(@Nonnull UiSession session,
                               @Nonnull ServerPlayer player,
                               @Nonnull BaseVillager villager,
                               long gameTime,
                               @Nonnull StatsPublishState state) {
        int demandVersion = demandDisplayVersion(villager, this.demandSignalService);
        VillagerDemandDisplaySnapshot snapshot = this.snapshotBuilder.buildDemandSnapshot(villager, gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerDemandSnapshotPacket(session.getSessionId(), snapshot));
        state.lastDemandDirtyCheckGameTime = gameTime;
        state.lastSentDemandVersion = demandVersion;
        session.markSnapshotSent(gameTime);
    }

    private static VillagerProfessionKey resolveProfessionKey(@Nonnull BaseVillager villager) {
        return villager.getProfession();
    }

    private static int demandDisplayVersion(@Nonnull BaseVillager villager,
                                            @Nonnull DemandSignalService demandSignalService) {
        // Demand display includes active shortages, which change when inventory fulfils or creates demand.
        return Objects.hash(villager.getSettlementsInventory().getInventoryVersion(), demandSignalService.getVersion(villager));
    }

    private static final class StatsPublishState {

        private long lastStatsSentGameTime;
        private long lastInventoryDirtyCheckGameTime;
        private int lastSentInventoryVersion = -1;
        private long lastCatalogDirtyCheckGameTime;
        private int lastSentCatalogVersion = -1;
        @Nullable
        private VillagerProfessionKey lastSentCatalogProfessionKey;
        private long lastDemandDirtyCheckGameTime;
        private int lastSentDemandVersion = -1;

    }

}
