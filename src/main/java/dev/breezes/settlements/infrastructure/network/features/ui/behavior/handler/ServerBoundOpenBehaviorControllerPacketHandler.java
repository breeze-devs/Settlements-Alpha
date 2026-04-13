package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSession;
import dev.breezes.settlements.application.ui.behavior.session.BehaviorControllerSessionService;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorControllerSnapshotBuilder;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundOpenBehaviorControllerPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundOpenBehaviorControllerPacketHandler implements ServerSidePacketHandler<ServerBoundOpenBehaviorControllerPacket> {

    private static final String UNAVAILABLE_REASON_KEY = "ui.settlements.behavior.unavailable";

    private final BehaviorControllerSessionService sessionService;
    private final BehaviorControllerSnapshotBuilder snapshotBuilder;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundOpenBehaviorControllerPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        // TODO(#behavior-ui-security): Add server-side inspect validation before session open:
        // 1) distance cap from player to villager
        // 2) optional LOS check
        // 3) debug/dev bypass flag for local testing

        Entity target = player.serverLevel().getEntity(packet.villagerEntityId());
        if (!(target instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
            PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerUnavailablePacket(-1L, UNAVAILABLE_REASON_KEY));
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        BehaviorControllerSession session = sessionService.startOrReplaceSession(player, packet.villagerEntityId(), gameTime);

        PacketDistributor.sendToPlayer(player,
                new ClientBoundOpenBehaviorControllerPacket(session.getSessionId(), packet.villagerEntityId()));

        PacketDistributor.sendToPlayer(player, new ClientBoundBehaviorControllerSnapshotPacket(session.getSessionId(), snapshotBuilder.build(villager, gameTime)));
        session.markSnapshotSent(gameTime);

        log.debug("Opened behavior controller sessionId={} for player={} villagerEntityId={}",
                session.getSessionId(), player.getUUID(), packet.villagerEntityId());
    }

}
