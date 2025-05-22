package dev.breezes.settlements.packet;

import dev.breezes.settlements.bubbles.packet.ClientBoundDisplayBubblePacket;
import dev.breezes.settlements.bubbles.packet.ClientBoundDisplayBubblePacketHandler;
import dev.breezes.settlements.bubbles.packet.ClientBoundRemoveBubblePacket;
import dev.breezes.settlements.bubbles.packet.ClientBoundRemoveBubblePacketHandler;
import lombok.CustomLog;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Map;

@CustomLog
public class ClientSidePacketReceiver {

    private static final ClientSidePacketReceiver INSTANCE = new ClientSidePacketReceiver();

    private static final Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> HANDLERS = Map.ofEntries(
            Map.entry(ClientBoundDisplayBubblePacket.class, new ClientBoundDisplayBubblePacketHandler()),
            Map.entry(ClientBoundRemoveBubblePacket.class, new ClientBoundRemoveBubblePacketHandler())
    );

    public static ClientSidePacketReceiver getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public void onReceivePacket(@Nonnull ClientBoundPacket data, @Nonnull IPayloadContext context) {
        context.enqueueWork(() -> {
                    PacketHandler<ClientBoundPacket> handler = (PacketHandler<ClientBoundPacket>) HANDLERS.get(data.getClass());
                    handler.onReceivePacket(context, data);
                })
                .exceptionally(e -> {
                    // Handle exception
                    context.disconnect(Component.literal("Failed to bubble: " + e.getMessage()));
                    return null;
                });
    }

}
