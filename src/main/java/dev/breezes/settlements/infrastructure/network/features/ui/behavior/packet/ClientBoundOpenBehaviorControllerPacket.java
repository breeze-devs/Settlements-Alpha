package dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ClientBoundOpenBehaviorControllerPacket(long sessionId,
                                                      int villagerEntityId) implements ClientBoundPacket {

    public static final Type<ClientBoundOpenBehaviorControllerPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_behavior_controller_open_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundOpenBehaviorControllerPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundOpenBehaviorControllerPacket::write, ClientBoundOpenBehaviorControllerPacket::decode);

    @Nonnull
    private static ClientBoundOpenBehaviorControllerPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ClientBoundOpenBehaviorControllerPacket(buffer.readLong(), buffer.readInt());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.sessionId);
        buffer.writeInt(this.villagerEntityId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
