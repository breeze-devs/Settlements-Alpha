package dev.breezes.settlements.application.ui.sync.session;

import dev.breezes.settlements.application.ui.sync.UiServerChannelDefinition;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ServerSide
@ServerScope
@NoArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class UiSessionRegistry {

    private static final long HEARTBEAT_TIMEOUT_TICKS = ClockTicks.seconds(4).getTicks();

    private final AtomicLong sessionIdGenerator = new AtomicLong(1000L);
    private final ConcurrentHashMap<UUID, UiSession> sessionsByPlayer = new ConcurrentHashMap<>();

    public UiSession startOrReplaceSession(@Nonnull ServerPlayer player,
                                           @Nonnull UiChannel channel,
                                           int villagerEntityId,
                                           long gameTime) {
        UUID playerUuid = player.getUUID();
        long sessionId = this.sessionIdGenerator.incrementAndGet();

        UiSession nextSession = UiSession.builder()
                .sessionId(sessionId)
                .channel(channel)
                .playerUuid(playerUuid)
                .villagerEntityId(villagerEntityId)
                .openedAtGameTime(gameTime)
                .lastSentGameTime(gameTime)
                .lastClientAckOrKeepAliveGameTime(gameTime)
                .build();

        UiSession previousSession = this.sessionsByPlayer.put(playerUuid, nextSession);
        if (previousSession == null) {
            log.debug("Created ui sessionId={} channel={} player={} villagerEntityId={}", sessionId, channel, playerUuid, villagerEntityId);
        } else {
            log.debug("Replaced ui session player={} oldSessionId={} newSessionId={} channel={} villagerEntityId={}",
                    playerUuid, previousSession.getSessionId(), sessionId, channel, villagerEntityId);
        }

        return nextSession;
    }

    public boolean isSessionStale(@Nonnull UUID playerUuid, long sessionId) {
        return this.getSession(playerUuid)
                .map(session -> session.getSessionId() != sessionId)
                .orElse(true);
    }

    public void closeSession(@Nonnull UUID playerUuid,
                             long sessionId,
                             @Nonnull Map<UiChannel, UiServerChannelDefinition> channelDefinitions) {
        this.sessionsByPlayer.computeIfPresent(playerUuid, (uuid, currentSession) -> {
            if (currentSession.getSessionId() != sessionId) {
                return currentSession;
            }
            getDefinition(currentSession, channelDefinitions).getSnapshotPublisher().onSessionClosed(currentSession);
            return null;
        });
    }

    public void recordHeartbeat(@Nonnull UUID playerUuid, long sessionId, long gameTime) {
        this.sessionsByPlayer.computeIfPresent(playerUuid, (uuid, currentSession) -> {
            if (currentSession.getSessionId() == sessionId) {
                currentSession.markClientKeepAlive(gameTime);
            }
            return currentSession;
        });
    }

    public Optional<UiSession> getSession(@Nonnull UUID playerUuid) {
        return Optional.ofNullable(this.sessionsByPlayer.get(playerUuid));
    }

    public Collection<UiSession> getAllSessions() {
        return List.copyOf(this.sessionsByPlayer.values());
    }

    public Collection<UiSession> getSessionsByChannel(@Nonnull UiChannel channel) {
        return this.getAllSessions().stream()
                .filter(session -> session.getChannel() == channel)
                .toList();
    }

    public void cleanupInvalidSessions(@Nonnull MinecraftServer server,
                                       long gameTime,
                                       @Nonnull Map<UiChannel, UiServerChannelDefinition> channelDefinitions) {
        for (UiSession session : this.getAllSessions()) {
            String unavailableReasonKey;
            ServerPlayer player = server.getPlayerList().getPlayer(session.getPlayerUuid());

            if (isSessionTimedOut(session, gameTime)) {
                unavailableReasonKey = getDefinition(session, channelDefinitions).getDefaultUnavailableReasonKey();
            } else if (player == null) {
                unavailableReasonKey = getDefinition(session, channelDefinitions).getDefaultUnavailableReasonKey();
            } else {
                Entity entity = player.serverLevel().getEntity(session.getVillagerEntityId());
                if (!(entity instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
                    unavailableReasonKey = getDefinition(session, channelDefinitions).getDefaultUnavailableReasonKey();
                } else {
                    unavailableReasonKey = getDefinition(session, channelDefinitions).getValidator().validate(villager).orElse(null);
                }
            }

            if (unavailableReasonKey == null) {
                continue;
            }

            boolean removed = this.sessionsByPlayer.remove(session.getPlayerUuid(), session);
            if (!removed) {
                continue;
            }

            log.debug("Removed ui sessionId={} channel={} player={}", session.getSessionId(), session.getChannel(), session.getPlayerUuid());
            getDefinition(session, channelDefinitions).getSnapshotPublisher().onSessionClosed(session);
            if (player != null) {
                PacketDistributor.sendToPlayer(player,
                        new ClientBoundUiUnavailablePacket(session.getChannel(), session.getSessionId(), unavailableReasonKey));
            }
        }
    }

    private static UiServerChannelDefinition getDefinition(@Nonnull UiSession session,
                                                           @Nonnull Map<UiChannel, UiServerChannelDefinition> definitions) {
        UiServerChannelDefinition definition = definitions.get(session.getChannel());
        if (definition == null) {
            throw new IllegalStateException("Missing UiServerChannelDefinition for " + session.getChannel());
        }
        return definition;
    }

    private static boolean isSessionTimedOut(@Nonnull UiSession session, long gameTime) {
        return (gameTime - session.getLastClientAckOrKeepAliveGameTime()) > HEARTBEAT_TIMEOUT_TICKS;
    }

}
