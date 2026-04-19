package dev.breezes.settlements.application.ui.stats.session;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsUnavailablePacket;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ServerSide
@ServerScope
@NoArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class VillagerStatsSessionService {

    private static final long HEARTBEAT_TIMEOUT_TICKS = 120L;
    private static final String UNAVAILABLE_CLEANUP_REASON_KEY = "ui.settlements.stats.unavailable";

    private final AtomicLong sessionIdGenerator = new AtomicLong(2000L);
    private final ConcurrentHashMap<UUID, VillagerStatsSession> sessionsByPlayer = new ConcurrentHashMap<>();

    public VillagerStatsSession startOrReplaceSession(@Nonnull ServerPlayer player,
                                                      int villagerEntityId,
                                                      long gameTime) {
        UUID playerUuid = player.getUUID();
        long sessionId = this.sessionIdGenerator.incrementAndGet();

        VillagerStatsSession nextSession = VillagerStatsSession.builder()
                .sessionId(sessionId)
                .playerUuid(playerUuid)
                .villagerEntityId(villagerEntityId)
                .openedAtGameTime(gameTime)
                .lastStatsSnapshotSentGameTime(gameTime)
                .lastInventorySnapshotSentGameTime(gameTime)
                .lastSentInventoryVersion(-1)
                .lastSentCatalogVersion(-1)
                .lastSentDemandVersion(-1)
                .lastClientAckOrKeepAliveGameTime(gameTime)
                .build();

        VillagerStatsSession previousSession = this.sessionsByPlayer.put(playerUuid, nextSession);
        if (previousSession == null) {
            log.debug("Created villager stats sessionId={} player={} villagerEntityId={}",
                    sessionId, playerUuid, villagerEntityId);
        } else {
            log.debug("Replaced villager stats session player={} oldSessionId={} newSessionId={} villagerEntityId={}",
                    playerUuid, previousSession.getSessionId(), sessionId, villagerEntityId);
        }

        return nextSession;
    }

    public boolean isSessionStale(@Nonnull UUID playerUuid, long sessionId) {
        return this.getSession(playerUuid)
                .map(session -> session.getSessionId() != sessionId)
                .orElse(true);
    }

    public void closeSession(@Nonnull UUID playerUuid, long sessionId) {
        this.sessionsByPlayer.computeIfPresent(playerUuid, (uuid, currentSession) ->
                currentSession.getSessionId() == sessionId ? null : currentSession
        );
    }

    public void recordHeartbeat(@Nonnull UUID playerUuid, long sessionId, long gameTime) {
        this.sessionsByPlayer.computeIfPresent(playerUuid, (uuid, currentSession) -> {
            if (currentSession.getSessionId() == sessionId) {
                currentSession.markClientKeepAlive(gameTime);
            }
            return currentSession;
        });
    }

    public Optional<VillagerStatsSession> getSession(@Nonnull UUID playerUuid) {
        return Optional.ofNullable(this.sessionsByPlayer.get(playerUuid));
    }

    public Collection<VillagerStatsSession> getAllSessions() {
        return List.copyOf(this.sessionsByPlayer.values());
    }

    public void cleanupInvalidSessions(@Nonnull MinecraftServer server, long gameTime) {
        for (VillagerStatsSession session : this.getAllSessions()) {
            String cleanupReason = null;
            ServerPlayer player;

            if (isSessionTimedOut(session, gameTime)) {
                cleanupReason = "heartbeat_timeout";
                player = server.getPlayerList().getPlayer(session.getPlayerUuid());
            } else {
                player = server.getPlayerList().getPlayer(session.getPlayerUuid());
                if (player == null) {
                    cleanupReason = "player_disconnected";
                } else {
                    Entity villager = player.serverLevel().getEntity(session.getVillagerEntityId());
                    if (villager == null) {
                        cleanupReason = "villager_missing_or_unloaded";
                    } else if (!(villager instanceof ISettlementsVillager)) {
                        cleanupReason = "target_not_settlements_villager";
                    } else if (!villager.isAlive() || villager.isRemoved()) {
                        cleanupReason = "villager_dead_or_removed";
                    }
                }
            }

            if (cleanupReason == null) {
                continue;
            }

            boolean removed = this.sessionsByPlayer.remove(session.getPlayerUuid(), session);
            if (removed) {
                log.debug("Removed villager stats sessionId={} player={} reason={}",
                        session.getSessionId(), session.getPlayerUuid(), cleanupReason);

                // Notify the client so it can close the screen immediately (player may be null if disconnected)
                if (player != null) {
                    PacketDistributor.sendToPlayer(player,
                            new ClientBoundVillagerStatsUnavailablePacket(session.getSessionId(), UNAVAILABLE_CLEANUP_REASON_KEY));
                }
            }
        }
    }

    private static boolean isSessionTimedOut(@Nonnull VillagerStatsSession session, long gameTime) {
        return (gameTime - session.getLastClientAckOrKeepAliveGameTime()) > HEARTBEAT_TIMEOUT_TICKS;
    }

}
