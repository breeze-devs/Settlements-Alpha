package dev.breezes.settlements.infrastructure.network.features.arrival.packet;

import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.Builder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * A settlement announcement for the receiving player, carrying everything the client shows of it.
 * <p>
 * The settlement's state travels with the announcement rather than being looked up on the client, since a disbanded
 * settlement has no summary left there.
 *
 * @param announcementType the kind of settlement announcement
 * @param dimension        the dimension storing the settlement, which its id names it within
 * @param settlementId     the settlement's id
 * @param name             the settlement's name when announced
 * @param status           the settlement's fading status, or null when it is not fading or announcementType is DISBANDED
 * @param previousName     the name the settlement had before the rename; present exactly when announcementType is RENAMED
 */
@Builder
public record ClientBoundSettlementArrivalPacket(
        SettlementAnnouncementType announcementType,
        ResourceKey<Level> dimension,
        UUID settlementId,
        String name,
        @Nullable SettlementSummary.Fading status,
        @Nullable String previousName
) implements ClientBoundPacket {

    public static final Type<ClientBoundSettlementArrivalPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_settlement_arrival_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundSettlementArrivalPacket> CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(SettlementAnnouncementType.class), ClientBoundSettlementArrivalPacket::announcementType,
            ResourceKey.streamCodec(Registries.DIMENSION), ClientBoundSettlementArrivalPacket::dimension,
            UUIDUtil.STREAM_CODEC, ClientBoundSettlementArrivalPacket::settlementId,
            ByteBufCodecs.STRING_UTF8, ClientBoundSettlementArrivalPacket::name,
            ByteBufCodecs.optional(SettlementSummary.Fading.STREAM_CODEC),
            packet -> Optional.ofNullable(packet.status()),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), packet -> Optional.ofNullable(packet.previousName()),
            (announcementType, dimension, settlementId, name, status, previousName) -> new ClientBoundSettlementArrivalPacket(
                    announcementType, dimension, settlementId, name, status.orElse(null), previousName.orElse(null)));

    public ClientBoundSettlementArrivalPacket {
        Validate.isTrue((previousName != null) == (announcementType == SettlementAnnouncementType.RENAMED),
                "A previous name accompanies exactly a RENAMED announcement, not %s with previous name %s", announcementType,
                previousName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
