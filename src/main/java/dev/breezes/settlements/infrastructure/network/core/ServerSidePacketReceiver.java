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
public class ServerSidePacketReceiver {

    private final Map<Class<?>, ServerSidePacketHandler<?>> handlers;

    public void onReceivePacket(@Nonnull ServerBoundPacket data, @Nonnull IPayloadContext context) {
        context.enqueueWork(() -> this.handleServerPacket(data, context))
                .exceptionally(e -> {
                    log.error("Failed to handle server-side packet", e);
                    context.disconnect(Component.literal("Failed to handle server packet: " + e.getMessage()));
                    return null;
                });
    }

    private void handleServerPacket(ServerBoundPacket data, IPayloadContext context) {
        ServerSidePacketHandler<?> handler = handlers.get(data.getClass());
        if (handler == null) {
            String message = "Missing server-side packet handler for packet type: %s".formatted(data.getClass().getName());
            CrashUtil.crash(new MissingPacketHandlerCrashReport(new IllegalStateException(message)));
            return;
        }

        // The multibinding map is keyed by the exact payload class, so once lookup succeeds
        // the erased handler can be safely re-associated with the packet instance being processed.
        @SuppressWarnings("unchecked")
        PacketHandler<ServerBoundPacket> typedHandler = (PacketHandler<ServerBoundPacket>) handler;

        typedHandler.onReceivePacket(context, data);
    }

}
