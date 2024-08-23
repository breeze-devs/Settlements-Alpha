package dev.breezes.settlements.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import javax.annotation.Nonnull;

public interface ISettlementsPacket {

    void serialize(@Nonnull FriendlyByteBuf buffer);

    void handle(@Nonnull CustomPayloadEvent.Context context);

}
