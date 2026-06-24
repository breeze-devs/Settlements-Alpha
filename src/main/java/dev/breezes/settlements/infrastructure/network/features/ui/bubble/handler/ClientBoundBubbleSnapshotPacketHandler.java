package dev.breezes.settlements.infrastructure.network.features.ui.bubble.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.shared.util.EntityUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundBubbleSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundBubbleSnapshotPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundBubbleSnapshotPacket packet) {
        Level level = context.player().level();
        Optional.ofNullable(level.getEntity(packet.villagerEntityId()))
                .flatMap(EntityUtil::convertToVillager)
                .ifPresent(villager -> villager.getBubbleManager().applySnapshot(packet.entries(), level.getGameTime()));
    }

}
