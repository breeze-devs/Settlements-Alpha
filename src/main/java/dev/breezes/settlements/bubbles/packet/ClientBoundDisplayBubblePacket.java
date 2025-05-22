package dev.breezes.settlements.bubbles.packet;

import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import dev.breezes.settlements.packet.ClientBoundPacket;
import dev.breezes.settlements.util.ResourceLocationUtil;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

@AllArgsConstructor
@Getter
@CustomLog
public class ClientBoundDisplayBubblePacket implements ClientBoundPacket {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new Gson());

    public static final Type<ClientBoundDisplayBubblePacket> ID = new Type<>(ResourceLocationUtil.mod("packet_display_bubble"));
    public static final StreamCodec<FriendlyByteBuf, ClientBoundDisplayBubblePacket> CODEC =
            CustomPacketPayload.codec(ClientBoundDisplayBubblePacket::write, ClientBoundDisplayBubblePacket::new);

    private final DisplayBubbleRequest request;

    private ClientBoundDisplayBubblePacket(@Nonnull FriendlyByteBuf buffer) {
        this(OBJECT_MAPPER.readValue(buffer.readUtf(), DisplayBubbleRequest.class));
    }

    public void write(@Nonnull FriendlyByteBuf buffer) {
        String packetJson = OBJECT_MAPPER.writeValueAsString(this.request);
        log.debug("Sending {} packet: {}", this.getClass().getSimpleName(), packetJson);
        buffer.writeUtf(packetJson);
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
