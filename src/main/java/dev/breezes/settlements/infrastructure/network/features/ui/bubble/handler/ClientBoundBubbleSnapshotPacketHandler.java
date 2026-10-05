package dev.breezes.settlements.infrastructure.network.features.ui.bubble.handler;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundBubbleSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundBubbleSnapshotPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundBubbleSnapshotPacket packet) {
        Level level = context.player().level();
        if (level.getEntity(packet.villagerEntityId()) instanceof BaseVillager villager) {
            villager.getBubbleManager().applySnapshot(packet.entries(), level.getGameTime());
        }
    }

}
