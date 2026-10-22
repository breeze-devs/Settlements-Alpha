package dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet;

import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.codec.BubbleEntrySnapshotCodec;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public record ClientBoundBubbleSnapshotPacket(int villagerEntityId,
                                              List<BubbleEntrySnapshot> entries) implements ClientBoundPacket {

    private static final int MAX_ENTRIES = 32;

    public static final Type<ClientBoundBubbleSnapshotPacket> ID = new Type<>(ResourceLocationUtil.mod("packet_bubble_snapshot_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundBubbleSnapshotPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundBubbleSnapshotPacket::write, ClientBoundBubbleSnapshotPacket::decode);

    public ClientBoundBubbleSnapshotPacket {
        entries = List.copyOf(entries);
    }

    private static ClientBoundBubbleSnapshotPacket decode(@Nonnull FriendlyByteBuf buffer) {
        int villagerEntityId = buffer.readInt();
        int entryCount = buffer.readVarInt();
        if (entryCount < 0 || entryCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid bubble snapshot entryCount: " + entryCount);
        }

        List<BubbleEntrySnapshot> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            entries.add(BubbleEntrySnapshotCodec.read(buffer));
        }
        return new ClientBoundBubbleSnapshotPacket(villagerEntityId, entries);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        if (this.entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many bubble snapshot entries: " + this.entries.size());
        }

        buffer.writeInt(this.villagerEntityId);
        buffer.writeVarInt(this.entries.size());
        for (BubbleEntrySnapshot entry : this.entries) {
            BubbleEntrySnapshotCodec.write(buffer, entry);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

}
