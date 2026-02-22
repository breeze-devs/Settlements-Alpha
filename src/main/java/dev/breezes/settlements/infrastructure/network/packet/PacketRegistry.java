package dev.breezes.settlements.infrastructure.network.packet;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundDisplayBubblePacket;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundRemoveBubblePacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nonnull;

public class PacketRegistry {

    public static void setupPackets(@Nonnull RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(SettlementsMod.MOD_ID).optional();

        // TODO: annotation-based packet registration & handling
        registrar.playToClient(ClientBoundDisplayBubblePacket.ID, ClientBoundDisplayBubblePacket.CODEC, ClientSidePacketReceiver.getInstance()::onReceivePacket);
        registrar.playToClient(ClientBoundRemoveBubblePacket.ID, ClientBoundRemoveBubblePacket.CODEC, ClientSidePacketReceiver.getInstance()::onReceivePacket);
    }

}
