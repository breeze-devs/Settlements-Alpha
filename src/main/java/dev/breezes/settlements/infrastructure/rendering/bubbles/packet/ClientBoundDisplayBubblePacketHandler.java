package dev.breezes.settlements.infrastructure.rendering.bubbles.packet;

import dev.breezes.settlements.infrastructure.rendering.bubbles.BubblePriority;
import dev.breezes.settlements.infrastructure.rendering.bubbles.registry.BubbleRegistry;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.infrastructure.network.packet.ClientSidePacketHandler;
import dev.breezes.settlements.shared.util.EntityUtil;
import lombok.CustomLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Optional;

@CustomLog
public class ClientBoundDisplayBubblePacketHandler implements ClientSidePacketHandler<ClientBoundDisplayBubblePacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundDisplayBubblePacket packet) {
        DisplayBubbleRequest request = packet.getRequest();
        log.debug("Received {} for entity ID: {}", packet.getClass().getSimpleName(), request.getEntityId());

        Player player = context.player();
        try (Level level = player.level()) {
            Optional.ofNullable(level.getEntity(request.getEntityId()))
                    .flatMap(EntityUtil::convertToVillager)
                    .ifPresent(villager -> this.addBubble(villager, request));
        } catch (Exception e) {
            log.error("Failed to remove bubble", e);
        }
    }

    private void addBubble(@Nonnull ISettlementsVillager villager, @Nonnull DisplayBubbleRequest request) {
        BubbleRegistry.getBubble(request)
                .ifPresent(bubble -> villager.getBubbleManager().addBubble(request.getBubbleId(), bubble, BubblePriority.DEFAULT.getPriority()));
    }

}
