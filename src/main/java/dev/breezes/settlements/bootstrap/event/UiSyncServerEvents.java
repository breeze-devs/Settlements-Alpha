package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ui.sync.UiServerChannelDefinition;
import dev.breezes.settlements.application.ui.sync.session.UiSession;
import dev.breezes.settlements.application.ui.sync.session.UiSessionRegistry;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.inject.Inject;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class UiSyncServerEvents {

    private static final int SNAPSHOT_PUBLISH_INTERVAL_TICKS = Ticks.of(10).getTicksAsInt();

    private final UiSessionRegistry sessionRegistry;
    private final Map<UiChannel, UiServerChannelDefinition> channelDefinitions;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();
        long dayTime = server.overworld().getDayTime();

        if (gameTime % SNAPSHOT_PUBLISH_INTERVAL_TICKS == 0) {
            this.sessionRegistry.cleanupInvalidSessions(server, gameTime, this.channelDefinitions);
        }

        for (UiSession session : this.sessionRegistry.getAllSessions()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.getPlayerUuid());
            if (player == null) {
                continue;
            }

            Entity entity = player.serverLevel().getEntity(session.getVillagerEntityId());
            UiServerChannelDefinition definition = this.channelDefinitions.get(session.getChannel());
            if (definition == null) {
                log.warn("Missing ui channel definition for {}", session.getChannel());
                continue;
            }

            if (!(entity instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
                PacketDistributor.sendToPlayer(player,
                        new ClientBoundUiUnavailablePacket(session.getChannel(), session.getSessionId(), definition.getDefaultUnavailableReasonKey()));
                this.sessionRegistry.closeSession(session.getPlayerUuid(), session.getSessionId());
                continue;
            }

            definition.getSnapshotPublisher().tick(session, player, villager, gameTime, dayTime);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        this.sessionRegistry.getSession(event.getEntity().getUUID())
                .ifPresent(session -> this.sessionRegistry.closeSession(session.getPlayerUuid(), session.getSessionId()));
    }

}
