package dev.breezes.settlements.infrastructure.network.features.schedule.packet;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;

/**
 * The village work rhythm's verdict together with the calendar day it speaks for.
 * <p>
 * The day travels with the verdict because a verdict is only ever true of one day, and the receiver has
 * its own clock.
 */
public record ClientBoundVillageDayTypePacket(long calendarDay,
                                              @Nonnull PlanDayType dayType) implements ClientBoundPacket {

    public static final Type<ClientBoundVillageDayTypePacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_village_day_type_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundVillageDayTypePacket> CODEC =
            CustomPacketPayload.codec(ClientBoundVillageDayTypePacket::write, ClientBoundVillageDayTypePacket::decode);

    private static ClientBoundVillageDayTypePacket decode(@Nonnull FriendlyByteBuf buffer) {
        long calendarDay = buffer.readLong();
        PlanDayType dayType = buffer.readEnum(PlanDayType.class);
        return new ClientBoundVillageDayTypePacket(calendarDay, dayType);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeLong(this.calendarDay);
        buffer.writeEnum(this.dayType);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
