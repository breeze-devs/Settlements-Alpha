package dev.breezes.settlements.infrastructure.network.features.ui.sync.handler;

import dev.breezes.settlements.application.ui.sync.UiServerChannelDefinition;
import dev.breezes.settlements.application.ui.sync.session.UiSession;
import dev.breezes.settlements.application.ui.sync.session.UiSessionRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundOpenUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundOpenUiPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundOpenUiPacketHandler implements ServerSidePacketHandler<ServerBoundOpenUiPacket> {

    private final UiSessionRegistry sessionRegistry;
    private final Map<UiChannel, UiServerChannelDefinition> channelDefinitions;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundOpenUiPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        UiServerChannelDefinition definition = this.channelDefinitions.get(packet.channel());
        if (definition == null) {
            log.warn("Received open packet for unregistered ui channel {}", packet.channel());
            return;
        }

        Entity target = player.serverLevel().getEntity(packet.villagerEntityId());
        if (!(target instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
            PacketDistributor.sendToPlayer(player, new ClientBoundUiUnavailablePacket(packet.channel(), 0L, definition.getDefaultUnavailableReasonKey()));
            return;
        }

        Optional<String> unavailableReason = definition.getValidator().validate(villager);
        if (unavailableReason.isPresent()) {
            PacketDistributor.sendToPlayer(player, new ClientBoundUiUnavailablePacket(packet.channel(), 0L, unavailableReason.get()));
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        long dayTime = player.serverLevel().getDayTime();
        UiSession session = this.sessionRegistry.startOrReplaceSession(player, packet.channel(), packet.villagerEntityId(), gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundOpenUiPacket(packet.channel(), session.getSessionId(), packet.villagerEntityId()));
        definition.getSnapshotPublisher().onSessionOpened(session, player, villager, gameTime, dayTime);
    }

}
