package dev.breezes.settlements.infrastructure.network.features.ui.dayplan.packet;

import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.codec.DayPlanSnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundDayPlanSnapshotPacket(long sessionId,
                                               @Nonnull DayPlanSnapshot snapshot) implements ClientBoundPacket {

    public static final Type<ClientBoundDayPlanSnapshotPacket> ID = new Type<>(ResourceLocationUtil.mod("packet_day_plan_snapshot_clientbound"));
    public static final StreamCodec<FriendlyByteBuf, ClientBoundDayPlanSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundDayPlanSnapshotPacket::write, ClientBoundDayPlanSnapshotPacket::decode);

    private static ClientBoundDayPlanSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundDayPlanSnapshotPacket(buffer.readLong(), DayPlanSnapshotCodec.read(buffer));
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        DayPlanSnapshotCodec.write(buffer, this.snapshot);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
