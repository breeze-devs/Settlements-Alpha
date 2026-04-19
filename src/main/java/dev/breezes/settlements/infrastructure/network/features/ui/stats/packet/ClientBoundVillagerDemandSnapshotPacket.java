package dev.breezes.settlements.infrastructure.network.features.ui.stats.packet;

import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.codec.VillagerDemandDisplaySnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundVillagerDemandSnapshotPacket(long sessionId,
                                                      @Nonnull VillagerDemandDisplaySnapshot snapshot) implements ClientBoundPacket {

    public static final Type<ClientBoundVillagerDemandSnapshotPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_villager_demand_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundVillagerDemandSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundVillagerDemandSnapshotPacket::write, ClientBoundVillagerDemandSnapshotPacket::decode);

    private static ClientBoundVillagerDemandSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        long sessionId = buffer.readLong();
        VillagerDemandDisplaySnapshot snapshot = VillagerDemandDisplaySnapshotCodec.read(buffer);
        return new ClientBoundVillagerDemandSnapshotPacket(sessionId, snapshot);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        VillagerDemandDisplaySnapshotCodec.write(buffer, this.snapshot);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
