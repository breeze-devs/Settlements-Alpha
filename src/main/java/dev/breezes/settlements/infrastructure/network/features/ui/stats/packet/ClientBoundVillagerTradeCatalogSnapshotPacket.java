package dev.breezes.settlements.infrastructure.network.features.ui.stats.packet;

import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.codec.VillagerTradeCatalogSnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundVillagerTradeCatalogSnapshotPacket(long sessionId,
                                                            @Nonnull VillagerTradeCatalogSnapshot snapshot) implements ClientBoundPacket {

    public static final Type<ClientBoundVillagerTradeCatalogSnapshotPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_villager_trade_catalog_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundVillagerTradeCatalogSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundVillagerTradeCatalogSnapshotPacket::write, ClientBoundVillagerTradeCatalogSnapshotPacket::decode);

    private static ClientBoundVillagerTradeCatalogSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        long sessionId = buffer.readLong();
        VillagerTradeCatalogSnapshot snapshot = VillagerTradeCatalogSnapshotCodec.read(buffer);
        return new ClientBoundVillagerTradeCatalogSnapshotPacket(sessionId, snapshot);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        VillagerTradeCatalogSnapshotCodec.write(buffer, this.snapshot);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
