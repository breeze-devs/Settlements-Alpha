package dev.breezes.settlements.application.ui.behavior.session;

import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.CustomLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ServerSide
@CustomLog
public final class BehaviorControllerSessionService {

    private static final long HEARTBEAT_TIMEOUT_TICKS = 120L;
    private static final BehaviorControllerSessionService INSTANCE = new BehaviorControllerSessionService();

    private final AtomicLong sessionIdGenerator = new AtomicLong(1000L);
    private final ConcurrentHashMap<UUID, BehaviorControllerSession> sessionsByPlayer = new ConcurrentHashMap<>();

    public static BehaviorControllerSessionService getInstance() {
        return INSTANCE;
    }

    public BehaviorControllerSession startOrReplaceSession(@Nonnull ServerPlayer player,
                                                           int villagerEntityId,
                                                           long gameTime) {
        UUID playerUuid = player.getUUID();
        long sessionId = this.sessionIdGenerator.incrementAndGet();

        BehaviorControllerSession nextSession = BehaviorControllerSession.builder()
                .sessionId(sessionId)
                .playerUuid(playerUuid)
                .villagerEntityId(villagerEntityId)
                .openedAtGameTime(gameTime)
                .lastSentGameTime(gameTime)
                .lastClientAckOrKeepAliveGameTime(gameTime)
                .build();

        BehaviorControllerSession previousSession = this.sessionsByPlayer.put(playerUuid, nextSession);
        if (previousSession == null) {
            log.debug("Created behavior controller sessionId={} player={} villagerEntityId={}",
                    sessionId, playerUuid, villagerEntityId);
        } else {
            log.debug("Replaced behavior controller session player={} oldSessionId={} newSessionId={} villagerEntityId={}",
                    playerUuid, previousSession.getSessionId(), sessionId, villagerEntityId);
        }

        return nextSession;
    }

    /**
     * A request is stale when the player has no active session,
     * or when the request sessionId does not match the active session.
     */
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

    public Optional<BehaviorControllerSession> getSession(@Nonnull UUID playerUuid) {
        return Optional.ofNullable(this.sessionsByPlayer.get(playerUuid));
    }

    public Collection<BehaviorControllerSession> getAllSessions() {
        return List.copyOf(this.sessionsByPlayer.values());
    }

    public void cleanupInvalidSessions(@Nonnull MinecraftServer server, long gameTime) {
        for (BehaviorControllerSession session : this.getAllSessions()) {
            String cleanupReason = null;

            if (isSessionTimedOut(session, gameTime)) {
                cleanupReason = "heartbeat_timeout";
            } else {
                ServerPlayer player = server.getPlayerList().getPlayer(session.getPlayerUuid());
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
                log.debug("Removed behavior controller sessionId={} player={} reason={}",
                        session.getSessionId(), session.getPlayerUuid(), cleanupReason);
            }
        }
    }

    private static boolean isSessionTimedOut(@Nonnull BehaviorControllerSession session, long gameTime) {
        return (gameTime - session.getLastClientAckOrKeepAliveGameTime()) > HEARTBEAT_TIMEOUT_TICKS;
    }

}
