package dev.breezes.settlements.infrastructure.network.core;

import dev.breezes.settlements.infrastructure.network.core.registry.ServerPacketHandlerRegistry;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.MissingPacketHandlerCrashReport;
import lombok.CustomLog;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.Optional;

@CustomLog
public class ServerSidePacketReceiver {

    private static final ServerSidePacketReceiver INSTANCE = new ServerSidePacketReceiver();

    public static ServerSidePacketReceiver getInstance() {
        return INSTANCE;
    }

    public void onReceivePacket(@Nonnull ServerBoundPacket data, @Nonnull IPayloadContext context) {
        context.enqueueWork(() -> this.handleServerPacket(data, context))
                .exceptionally(e -> {
                    log.error("Failed to handle server-side packet", e);
                    context.disconnect(Component.literal("Failed to handle server packet: " + e.getMessage()));
                    return null;
                });
    }

    private void handleServerPacket(ServerBoundPacket data, IPayloadContext context) {
        Optional<ServerSidePacketHandler<? extends ServerBoundPacket>> handler = ServerPacketHandlerRegistry.get(data.getClass());
        if (handler.isEmpty()) {
            String message = "Missing server-side packet handler for packet type: %s".formatted(data.getClass().getName());
            CrashUtil.crash(new MissingPacketHandlerCrashReport(new IllegalStateException(message)));
            return;
        }

        @SuppressWarnings("unchecked")
        PacketHandler<ServerBoundPacket> typedHandler = (PacketHandler<ServerBoundPacket>) handler.get();

        typedHandler.onReceivePacket(context, data);
    }

}
