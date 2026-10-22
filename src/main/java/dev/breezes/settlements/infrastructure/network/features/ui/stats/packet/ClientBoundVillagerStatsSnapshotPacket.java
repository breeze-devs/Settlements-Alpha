package dev.breezes.settlements.infrastructure.network.features.ui.stats.packet;

import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.codec.VillagerStatsSnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundVillagerStatsSnapshotPacket(long sessionId,
                                                     VillagerStatsSnapshot snapshot) implements ClientBoundPacket {

    public static final Type<ClientBoundVillagerStatsSnapshotPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_villager_stats_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundVillagerStatsSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundVillagerStatsSnapshotPacket::write, ClientBoundVillagerStatsSnapshotPacket::decode);

    private static ClientBoundVillagerStatsSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        long sessionId = buffer.readLong();
        VillagerStatsSnapshot snapshot = VillagerStatsSnapshotCodec.read(buffer);
        return new ClientBoundVillagerStatsSnapshotPacket(sessionId, snapshot);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        VillagerStatsSnapshotCodec.write(buffer, this.snapshot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
