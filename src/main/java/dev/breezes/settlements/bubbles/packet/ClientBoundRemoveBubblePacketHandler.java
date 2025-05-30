package dev.breezes.settlements.bubbles.packet;

import dev.breezes.settlements.packet.ClientSidePacketHandler;
import dev.breezes.settlements.util.EntityUtil;
import lombok.CustomLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Optional;

@CustomLog
public class ClientBoundRemoveBubblePacketHandler implements ClientSidePacketHandler<ClientBoundRemoveBubblePacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundRemoveBubblePacket packet) {
        RemoveBubbleRequest request = packet.getRequest();
        log.debug("Received {} for entity ID: {}", packet.getClass().getSimpleName(), request.getEntityId());

        Player player = context.player();
        try (Level level = player.level()) {
            Optional.ofNullable(level.getEntity(request.getEntityId()))
                    .flatMap(EntityUtil::convertToVillager)
                    .ifPresent(villager -> villager.getBubbleManager().removeBubble(request.getBubbleId()));
        } catch (Exception e) {
            log.error("Failed to remove bubble", e);
        }
    }

}
