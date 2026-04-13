package dev.breezes.settlements.infrastructure.network.core;

import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.MissingPacketHandlerCrashReport;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientSidePacketReceiver {

    private final Map<Class<?>, ClientSidePacketHandler<?>> handlers;

    public void onReceivePacket(@Nonnull ClientBoundPacket data, @Nonnull IPayloadContext context) {
        context.enqueueWork(() -> this.handleClientPacket(data, context))
                .exceptionally(e -> {
                    log.error("Failed to handle client-side packet", e);
                    context.disconnect(Component.literal("Failed to handle client packet: " + e.getMessage()));
                    return null;
                });
    }

    private void handleClientPacket(ClientBoundPacket data, IPayloadContext context) {
        ClientSidePacketHandler<?> handler = handlers.get(data.getClass());
        if (handler == null) {
            String message = "Missing client-side packet handler for packet type: %s".formatted(data.getClass().getName());
            CrashUtil.crash(new MissingPacketHandlerCrashReport(new IllegalStateException(message)));
            return;
        }

        // The multibinding map is keyed by the exact payload class, so once lookup succeeds
        // the erased handler can be safely re-associated with the packet instance being processed.
        @SuppressWarnings("unchecked")
        PacketHandler<ClientBoundPacket> typedHandler = (PacketHandler<ClientBoundPacket>) handler;

        typedHandler.onReceivePacket(context, data);
    }

}
