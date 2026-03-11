package dev.breezes.settlements.infrastructure.network.core;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.network.core.registry.PacketHandlerAnnotationProcessor;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundHeartbeatAckBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundCloseBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundHeartbeatBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundDisplayBubblePacket;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundRemoveBubblePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nonnull;

public class PacketRegistry {

    public static void bindPacketHandlers(@Nonnull RegisterPayloadHandlersEvent event) {
        // Initialize packet & handler bindings
        PacketHandlerAnnotationProcessor.initialize();

        // Register with Minecraft registrar
        PayloadRegistrar registrar = event.registrar(SettlementsMod.MOD_ID).optional();

        registerClient(registrar, ClientBoundDisplayBubblePacket.ID, ClientBoundDisplayBubblePacket.CODEC);
        registerClient(registrar, ClientBoundRemoveBubblePacket.ID, ClientBoundRemoveBubblePacket.CODEC);

        registerServer(registrar, ServerBoundOpenBehaviorControllerPacket.ID, ServerBoundOpenBehaviorControllerPacket.CODEC);
        registerServer(registrar, ServerBoundCloseBehaviorControllerPacket.ID, ServerBoundCloseBehaviorControllerPacket.CODEC);
        registerServer(registrar, ServerBoundHeartbeatBehaviorControllerPacket.ID, ServerBoundHeartbeatBehaviorControllerPacket.CODEC);

        registerClient(registrar, ClientBoundOpenBehaviorControllerPacket.ID, ClientBoundOpenBehaviorControllerPacket.CODEC);
        registerClient(registrar, ClientBoundBehaviorControllerSnapshotPacket.ID, ClientBoundBehaviorControllerSnapshotPacket.CODEC);
        registerClient(registrar, ClientBoundHeartbeatAckBehaviorControllerPacket.ID, ClientBoundHeartbeatAckBehaviorControllerPacket.CODEC);
        registerClient(registrar, ClientBoundBehaviorControllerUnavailablePacket.ID, ClientBoundBehaviorControllerUnavailablePacket.CODEC);
    }

    private static <T extends ClientBoundPacket> void registerClient(@Nonnull PayloadRegistrar registrar,
                                                                     @Nonnull CustomPacketPayload.Type<T> packetId,
                                                                     @Nonnull StreamCodec<FriendlyByteBuf, T> codec) {
        registrar.playToClient(packetId, codec, ClientSidePacketReceiver.getInstance()::onReceivePacket);
    }

    private static <T extends ServerBoundPacket> void registerServer(@Nonnull PayloadRegistrar registrar,
                                                                     @Nonnull CustomPacketPayload.Type<T> packetId,
                                                                     @Nonnull StreamCodec<FriendlyByteBuf, T> codec) {
        registrar.playToServer(packetId, codec, ServerSidePacketReceiver.getInstance()::onReceivePacket);
    }

}
