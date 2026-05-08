package dev.breezes.settlements.infrastructure.network.core;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.packet.ClientBoundDayPlanSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundHeartbeatAckVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundCloseVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundHeartbeatVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundHeartbeatAckUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundOpenUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ClientBoundUiUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundCloseUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundHeartbeatUiPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundOpenUiPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nonnull;

public class PacketRegistry {

    public static void bindPacketHandlers(@Nonnull RegisterPayloadHandlersEvent event) {
        // Register with Minecraft registrar
        PayloadRegistrar registrar = event.registrar(SettlementsMod.MOD_ID).optional();

        registerClient(registrar, ClientBoundSettlementDebugPacket.ID, ClientBoundSettlementDebugPacket.CODEC);
        registerClient(registrar, ClientBoundBubbleSnapshotPacket.ID, ClientBoundBubbleSnapshotPacket.CODEC);

        // Shared UiSync lifecycle packets
        registerServer(registrar, ServerBoundOpenUiPacket.ID, ServerBoundOpenUiPacket.CODEC);
        registerServer(registrar, ServerBoundCloseUiPacket.ID, ServerBoundCloseUiPacket.CODEC);
        registerServer(registrar, ServerBoundHeartbeatUiPacket.ID, ServerBoundHeartbeatUiPacket.CODEC);

        registerClient(registrar, ClientBoundOpenUiPacket.ID, ClientBoundOpenUiPacket.CODEC);
        registerClient(registrar, ClientBoundHeartbeatAckUiPacket.ID, ClientBoundHeartbeatAckUiPacket.CODEC);
        registerClient(registrar, ClientBoundUiUnavailablePacket.ID, ClientBoundUiUnavailablePacket.CODEC);
        registerClient(registrar, ClientBoundDayPlanSnapshotPacket.ID, ClientBoundDayPlanSnapshotPacket.CODEC);

        // Villager stats UI packets
        registerServer(registrar, ServerBoundOpenVillagerStatsPacket.ID, ServerBoundOpenVillagerStatsPacket.CODEC);
        registerServer(registrar, ServerBoundCloseVillagerStatsPacket.ID, ServerBoundCloseVillagerStatsPacket.CODEC);
        registerServer(registrar, ServerBoundHeartbeatVillagerStatsPacket.ID, ServerBoundHeartbeatVillagerStatsPacket.CODEC);

        registerClient(registrar, ClientBoundOpenVillagerStatsPacket.ID, ClientBoundOpenVillagerStatsPacket.CODEC);
        registerClient(registrar, ClientBoundVillagerStatsSnapshotPacket.ID, ClientBoundVillagerStatsSnapshotPacket.CODEC);
        registerClient(registrar, ClientBoundVillagerInventorySnapshotPacket.ID, ClientBoundVillagerInventorySnapshotPacket.CODEC);
        registerClient(registrar, ClientBoundVillagerTradeCatalogSnapshotPacket.ID, ClientBoundVillagerTradeCatalogSnapshotPacket.CODEC);
        registerClient(registrar, ClientBoundVillagerDemandSnapshotPacket.ID, ClientBoundVillagerDemandSnapshotPacket.CODEC);
        registerClient(registrar, ClientBoundHeartbeatAckVillagerStatsPacket.ID, ClientBoundHeartbeatAckVillagerStatsPacket.CODEC);
        registerClient(registrar, ClientBoundVillagerStatsUnavailablePacket.ID, ClientBoundVillagerStatsUnavailablePacket.CODEC);
    }

    private static <T extends ClientBoundPacket> void registerClient(@Nonnull PayloadRegistrar registrar,
                                                                     @Nonnull CustomPacketPayload.Type<T> packetId,
                                                                     @Nonnull StreamCodec<FriendlyByteBuf, T> codec) {
        // NeoForge captures this callback during mod setup, but the client receiver only becomes
        // available after the Dagger client subcomponent is built. Resolving it lazily keeps the
        // lifecycle edge explicit and avoids forcing earlier component initialization.
        registrar.playToClient(packetId, codec,
                (packet, context) -> SettlementsDagger.client().clientSidePacketReceiver().onReceivePacket(packet, context));
    }

    private static <T extends ServerBoundPacket> void registerServer(@Nonnull PayloadRegistrar registrar,
                                                                     @Nonnull CustomPacketPayload.Type<T> packetId,
                                                                     @Nonnull StreamCodec<FriendlyByteBuf, T> codec) {
        // NeoForge captures this callback during mod setup, but the server receiver only becomes
        // available after the Dagger server subcomponent is built. Resolving it lazily keeps the
        // lifecycle edge explicit and avoids forcing earlier component initialization.
        registrar.playToServer(packetId, codec,
                (packet, context) -> SettlementsDagger.serverOrThrow().serverSidePacketReceiver().onReceivePacket(packet, context));
    }

}
