package dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.codec.BehaviorControllerSnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundBehaviorControllerSnapshotPacket(long sessionId,
                                                          @Nonnull BehaviorControllerSnapshot snapshot) implements ClientBoundPacket {

    public static final Type<ClientBoundBehaviorControllerSnapshotPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_behavior_controller_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundBehaviorControllerSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundBehaviorControllerSnapshotPacket::write, ClientBoundBehaviorControllerSnapshotPacket::decode);

    private static ClientBoundBehaviorControllerSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        long sessionId = buffer.readLong();
        BehaviorControllerSnapshot snapshot = BehaviorControllerSnapshotCodec.read(buffer);
        return new ClientBoundBehaviorControllerSnapshotPacket(sessionId, snapshot);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        BehaviorControllerSnapshotCodec.write(buffer, this.snapshot);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
