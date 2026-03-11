package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorControllerSnapshotBuilder;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
import lombok.CustomLog;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public final class BehaviorControllerServerEvents {

    private static final int SNAPSHOT_PUBLISH_INTERVAL_TICKS = 10;
    private static final String UNAVAILABLE_REASON_KEY = "ui.settlements.behavior.unavailable";

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % SNAPSHOT_PUBLISH_INTERVAL_TICKS != 0) {
            return;
        }

        BehaviorControllerSessionService sessions = BehaviorControllerSessionService.getInstance();
        sessions.cleanupInvalidSessions(event.getServer(), gameTime);

        BehaviorControllerSnapshotBuilder builder = BehaviorControllerSnapshotBuilder.getInstance();
        for (BehaviorControllerSession session : sessions.getAllSessions()) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.getPlayerUuid());
            if (player == null) {
                continue;
            }

            Entity entity = player.serverLevel().getEntity(session.getVillagerEntityId());
            if (!(entity instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
                PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerUnavailablePacket(session.getSessionId(), UNAVAILABLE_REASON_KEY));
                sessions.closeSession(session.getPlayerUuid(), session.getSessionId());
                continue;
            }

            PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerSnapshotPacket(session.getSessionId(), builder.build(villager, gameTime)));
            session.markSnapshotSent(gameTime);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BehaviorControllerSessionService sessions = BehaviorControllerSessionService.getInstance();
        sessions.getSession(event.getEntity().getUUID())
                .ifPresent(session -> sessions.closeSession(session.getPlayerUuid(), session.getSessionId()));
    }

}
