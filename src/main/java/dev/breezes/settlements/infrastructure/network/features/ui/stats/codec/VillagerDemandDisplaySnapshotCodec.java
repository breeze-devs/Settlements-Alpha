package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.ui.stats.model.DemandDisplayEntry;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatchCodec;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class VillagerDemandDisplaySnapshotCodec {

    public static VillagerDemandDisplaySnapshot read(@Nonnull FriendlyByteBuf buffer) {
        int entryCount = buffer.readVarInt();
        List<DemandDisplayEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            ActiveDemand activeDemand = null;
            if (buffer.readBoolean()) {
                activeDemand = ActiveDemand.builder()
                        .match(readItemMatch(buffer))
                        .desiredCount(buffer.readVarInt())
                        .priority(buffer.readVarInt())
                        .basePricePerUnit(buffer.readVarInt())
                        .origin(buffer.readEnum(ActiveDemand.Origin.class))
                        .build();
            }

            entries.add(DemandDisplayEntry.builder()
                    .id(buffer.readUtf())
                    .match(readItemMatch(buffer))
                    .desiredMinCount(buffer.readVarInt())
                    .basePricePerUnit(buffer.readVarInt())
                    .basePriority(buffer.readVarInt())
                    .activeDemand(activeDemand)
                    .build());
        }

        return new VillagerDemandDisplaySnapshot(entries);
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull VillagerDemandDisplaySnapshot snapshot) {
        buffer.writeVarInt(snapshot.entries().size());
        for (DemandDisplayEntry entry : snapshot.entries()) {
            buffer.writeBoolean(entry.active());
            if (entry.activeDemand() != null) {
                writeItemMatch(buffer, entry.activeDemand().match());
                buffer.writeVarInt(entry.activeDemand().desiredCount());
                buffer.writeVarInt(entry.activeDemand().priority());
                buffer.writeVarInt(entry.activeDemand().basePricePerUnit());
                buffer.writeEnum(entry.activeDemand().origin());
            }
            buffer.writeUtf(entry.id());
            writeItemMatch(buffer, entry.match());
            buffer.writeVarInt(entry.desiredMinCount());
            buffer.writeVarInt(entry.basePricePerUnit());
            buffer.writeVarInt(entry.basePriority());
        }
    }

    private static ItemMatch readItemMatch(@Nonnull FriendlyByteBuf buffer) {
        return ItemMatchCodec.STREAM_CODEC.decode(buffer);
    }

    private static void writeItemMatch(@Nonnull FriendlyByteBuf buffer, @Nonnull ItemMatch match) {
        ItemMatchCodec.STREAM_CODEC.encode(buffer, match);
    }

}
