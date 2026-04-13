package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSession;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSessionService;
import dev.breezes.settlements.application.ui.stats.snapshot.VillagerStatsSnapshotBuilder;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class VillagerStatsServerEvents {

    private static final int STATS_SNAPSHOT_INTERVAL_TICKS = Ticks.seconds(0.5).getTicksAsInt();
    private static final int INVENTORY_SNAPSHOT_INTERVAL_TICKS = Ticks.seconds(3).getTicksAsInt();

    private final VillagerStatsSessionService sessionService;
    private final VillagerStatsSnapshotBuilder snapshotBuilder;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();

        boolean isStatsTick = gameTime % STATS_SNAPSHOT_INTERVAL_TICKS == 0;
        boolean isInventoryTick = gameTime % INVENTORY_SNAPSHOT_INTERVAL_TICKS == 0;
        if (!isStatsTick && !isInventoryTick) {
            return;
        }

        if (isStatsTick) {
            this.sessionService.cleanupInvalidSessions(event.getServer(), gameTime);
        }

        for (VillagerStatsSession session : this.sessionService.getAllSessions()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.getPlayerUuid());
            if (player == null) {
                continue;
            }

            // cleanupInvalidSessions() already removed dead/removed/missing villagers above,
            // so this only needs the instanceof cast guard
            Entity entity = player.serverLevel().getEntity(session.getVillagerEntityId());
            if (!(entity instanceof BaseVillager villager)) {
                continue;
            }

            if (isStatsTick) {
                VillagerStatsSnapshot statsSnapshot = this.snapshotBuilder.buildStats(villager, gameTime);
                PacketDistributor.sendToPlayer(player,
                        new ClientBoundVillagerStatsSnapshotPacket(session.getSessionId(), statsSnapshot));
                session.markStatsSnapshotSent(gameTime);
            }

            if (isInventoryTick) {
                int currentInventoryVersion = villager.getSettlementsInventory().getInventoryVersion();
                if (currentInventoryVersion != session.getLastSentInventoryVersion()) {
                    VillagerInventorySnapshot inventorySnapshot = this.snapshotBuilder.buildInventory(villager);
                    PacketDistributor.sendToPlayer(player,
                            new ClientBoundVillagerInventorySnapshotPacket(session.getSessionId(), inventorySnapshot));
                    session.markInventorySnapshotSent(gameTime, currentInventoryVersion);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        this.sessionService.getSession(event.getEntity().getUUID())
                .ifPresent(session -> this.sessionService.closeSession(session.getPlayerUuid(), session.getSessionId()));
    }

}
