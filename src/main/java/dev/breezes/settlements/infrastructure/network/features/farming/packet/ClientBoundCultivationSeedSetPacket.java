package dev.breezes.settlements.infrastructure.network.features.farming.packet;

import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Denormalizes the server's accepted-seed catalog (crop definitions are datapack-backed and do not
 * exist client-side) into the item ids the client needs to answer "is this stack a seed" on its own.
 */
public record ClientBoundCultivationSeedSetPacket(
        @Nonnull List<ResourceLocation> seedItemIds) implements ClientBoundPacket {

    /**
     * Generous ceiling on a datapack's plausible crop catalog size.
     */
    static final int MAX_ENTRIES = 512;

    public static final Type<ClientBoundCultivationSeedSetPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_cultivation_seed_set_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundCultivationSeedSetPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundCultivationSeedSetPacket::write, ClientBoundCultivationSeedSetPacket::decode);

    public ClientBoundCultivationSeedSetPacket {
        seedItemIds = List.copyOf(seedItemIds);
    }

    private static ClientBoundCultivationSeedSetPacket decode(@Nonnull FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid cultivation seed set entryCount: " + count);
        }

        List<ResourceLocation> seedItemIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            seedItemIds.add(buffer.readResourceLocation());
        }
        return new ClientBoundCultivationSeedSetPacket(seedItemIds);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        if (this.seedItemIds.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many cultivation seed set entries: " + this.seedItemIds.size());
        }

        buffer.writeVarInt(this.seedItemIds.size());
        for (ResourceLocation seedItemId : this.seedItemIds) {
            buffer.writeResourceLocation(seedItemId);
        }
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
