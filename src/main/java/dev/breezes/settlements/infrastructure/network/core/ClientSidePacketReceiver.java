package dev.breezes.settlements.infrastructure.network.core;

import dev.breezes.settlements.infrastructure.network.core.registry.ClientPacketHandlerRegistry;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.MissingPacketHandlerCrashReport;
import lombok.CustomLog;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Optional;

@CustomLog
public class ClientSidePacketReceiver {

    private static final ClientSidePacketReceiver INSTANCE = new ClientSidePacketReceiver();

    public static ClientSidePacketReceiver getInstance() {
        return INSTANCE;
    }

    public void onReceivePacket(@Nonnull ClientBoundPacket data, @Nonnull IPayloadContext context) {
        context.enqueueWork(() -> this.handleClientPacket(data, context))
                .exceptionally(e -> {
                    log.error("Failed to handle client-side packet", e);
                    context.disconnect(Component.literal("Failed to handle client packet: " + e.getMessage()));
                    return null;
                });
    }

    private void handleClientPacket(ClientBoundPacket data, IPayloadContext context) {
        Optional<ClientSidePacketHandler<? extends ClientBoundPacket>> handler = ClientPacketHandlerRegistry.get(data.getClass());
        if (handler.isEmpty()) {
            String message = "Missing client-side packet handler for packet type: %s".formatted(data.getClass().getName());
            CrashUtil.crash(new MissingPacketHandlerCrashReport(new IllegalStateException(message)));
            return;
        }

        @SuppressWarnings("unchecked")
        PacketHandler<ClientBoundPacket> typedHandler = (PacketHandler<ClientBoundPacket>) handler.get();

        typedHandler.onReceivePacket(context, data);
    }

}
