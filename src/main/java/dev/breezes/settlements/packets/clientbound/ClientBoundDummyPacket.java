package dev.breezes.settlements.packets.clientbound;

import dev.breezes.settlements.packets.AbstractSettlementsPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import javax.annotation.Nonnull;

/**
 * Example packet & registration
 */
@Deprecated
public class ClientBoundDummyPacket extends AbstractSettlementsPacket {

    public ClientBoundDummyPacket(@Nonnull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void serialize(@Nonnull FriendlyByteBuf buffer) {

    }

    @Override
    public void handle(@Nonnull CustomPayloadEvent.Context context) {

    }

}
