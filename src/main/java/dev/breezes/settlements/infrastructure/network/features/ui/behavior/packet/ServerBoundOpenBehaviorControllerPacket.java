package dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

public record ServerBoundOpenBehaviorControllerPacket(int villagerEntityId) implements ServerBoundPacket {

    public static final Type<ServerBoundOpenBehaviorControllerPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_behavior_controller_open_serverbound"));

    public static final StreamCodec<FriendlyByteBuf, ServerBoundOpenBehaviorControllerPacket> CODEC =
            CustomPacketPayload.codec(ServerBoundOpenBehaviorControllerPacket::write, ServerBoundOpenBehaviorControllerPacket::decode);

    @Nonnull
    private static ServerBoundOpenBehaviorControllerPacket decode(@Nonnull FriendlyByteBuf buffer) {
        return new ServerBoundOpenBehaviorControllerPacket(buffer.readInt());
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeInt(this.villagerEntityId);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
