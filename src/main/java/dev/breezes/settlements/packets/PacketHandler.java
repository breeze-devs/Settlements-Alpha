package dev.breezes.settlements.packets;

import dev.breezes.settlements.packets.clientbound.ClientBoundDummyPacket;
import dev.breezes.settlements.util.ResourceLocationUtil;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import javax.annotation.Nonnull;

public class PacketHandler {

    private static final String CHANNEL_RESOURCE_LOCATION = "main_channel";

    private static final SimpleChannel DEFAULT_CHANNEL = ChannelBuilder.named(ResourceLocationUtil.mod(CHANNEL_RESOURCE_LOCATION))
            .serverAcceptedVersions((status, version) -> true)
            .clientAcceptedVersions((status, version) -> true)
            .simpleChannel();

    /**
     * Called upon ?
     */
    public static void registerPackets() {
        // TODO: remove example packet after implementing actual packets
        DEFAULT_CHANNEL.messageBuilder(ClientBoundDummyPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundDummyPacket::serialize)
                .decoder(ClientBoundDummyPacket::new)
                .consumer(ClientBoundDummyPacket::handle)
                .add();
    }

    public static void sendToAllClients(@Nonnull ISettlementsPacket packet) {
        DEFAULT_CHANNEL.send(packet, PacketDistributor.ALL.noArg());
    }

}
