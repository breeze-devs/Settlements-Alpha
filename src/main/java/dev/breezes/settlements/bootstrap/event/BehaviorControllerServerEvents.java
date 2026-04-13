package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorControllerSnapshotBuilder;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
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
public final class BehaviorControllerServerEvents {

    private static final int SNAPSHOT_PUBLISH_INTERVAL_TICKS = 10;
    private static final String UNAVAILABLE_REASON_KEY = "ui.settlements.behavior.unavailable";

    private final BehaviorControllerSessionService sessionService;
    private final BehaviorControllerSnapshotBuilder snapshotBuilder;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % SNAPSHOT_PUBLISH_INTERVAL_TICKS != 0) {
            return;
        }

        this.sessionService.cleanupInvalidSessions(event.getServer(), gameTime);

        for (BehaviorControllerSession session : this.sessionService.getAllSessions()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.getPlayerUuid());
            if (player == null) {
                continue;
            }

            Entity entity = player.serverLevel().getEntity(session.getVillagerEntityId());
            if (!(entity instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
                PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerUnavailablePacket(session.getSessionId(), UNAVAILABLE_REASON_KEY));
                this.sessionService.closeSession(session.getPlayerUuid(), session.getSessionId());
                continue;
            }

            PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerSnapshotPacket(session.getSessionId(), this.snapshotBuilder.build(villager, gameTime)));
            session.markSnapshotSent(gameTime);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        this.sessionService.getSession(event.getEntity().getUUID())
                .ifPresent(session -> this.sessionService.closeSession(session.getPlayerUuid(), session.getSessionId()));
    }

}
