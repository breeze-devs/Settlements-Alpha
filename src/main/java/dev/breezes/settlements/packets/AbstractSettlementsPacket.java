package dev.breezes.settlements.packets;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;

public abstract class AbstractSettlementsPacket implements ISettlementsPacket {

    public AbstractSettlementsPacket(@Nonnull FriendlyByteBuf buffer) {
        // Empty constructor to enforce deserialization in subclasses
    }

}
