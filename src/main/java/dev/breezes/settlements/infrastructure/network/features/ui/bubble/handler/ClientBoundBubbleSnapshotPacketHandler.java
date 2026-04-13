package dev.breezes.settlements.infrastructure.network.features.ui.bubble.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.shared.util.EntityUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundBubbleSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundBubbleSnapshotPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundBubbleSnapshotPacket packet) {
        log.debug("Received {} for villagerEntityId={} entryCount={}",
                packet.getClass().getSimpleName(), packet.villagerEntityId(), packet.entries().size());

        Level level = context.player().level();
        Optional.ofNullable(level.getEntity(packet.villagerEntityId()))
                .flatMap(EntityUtil::convertToVillager)
                .ifPresentOrElse(
                        villager -> villager.getBubbleManager().applySnapshot(packet.entries(), level.getGameTime()),
                        () -> log.debug("Ignoring bubble snapshot because villager {} is not available on the client", packet.villagerEntityId())
                );
    }

}
