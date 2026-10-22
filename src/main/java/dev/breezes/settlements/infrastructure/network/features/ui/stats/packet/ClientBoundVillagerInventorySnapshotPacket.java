package dev.breezes.settlements.infrastructure.network.features.ui.stats.packet;

import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.codec.VillagerInventorySnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundVillagerInventorySnapshotPacket(long sessionId,
                                                         VillagerInventorySnapshot inventory) implements ClientBoundPacket {

    public static final Type<ClientBoundVillagerInventorySnapshotPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_villager_stats_inventory_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundVillagerInventorySnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundVillagerInventorySnapshotPacket::write, ClientBoundVillagerInventorySnapshotPacket::decode);

    private static ClientBoundVillagerInventorySnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        long sessionId = buffer.readLong();
        VillagerInventorySnapshot inventory = VillagerInventorySnapshotCodec.read(buffer);
        return new ClientBoundVillagerInventorySnapshotPacket(sessionId, inventory);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        VillagerInventorySnapshotCodec.write(buffer, this.inventory);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
