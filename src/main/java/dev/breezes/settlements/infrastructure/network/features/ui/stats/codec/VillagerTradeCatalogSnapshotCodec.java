package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatchCodec;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class VillagerTradeCatalogSnapshotCodec {

    public static VillagerTradeCatalogSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        int offerCount = buffer.readVarInt();
        List<OfferEntry> offers = new ArrayList<>(offerCount);
        for (int i = 0; i < offerCount; i++) {
            offers.add(OfferEntry.builder()
                    .id(buffer.readUtf())
                    .match(readItemMatch(buffer))
                    .bundleSize(buffer.readVarInt())
                    .basePrice(buffer.readVarInt())
                    .priceJitter(buffer.readVarInt())
                    .surplusThreshold(buffer.readVarInt())
                    .build());
        }

        return new VillagerTradeCatalogSnapshot(offers);
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull VillagerTradeCatalogSnapshot snapshot) {
        buffer.writeVarInt(snapshot.offers().size());
        for (OfferEntry offer : snapshot.offers()) {
            buffer.writeUtf(offer.id());
            writeItemMatch(buffer, offer.match());
            buffer.writeVarInt(offer.bundleSize());
            buffer.writeVarInt(offer.basePrice());
            buffer.writeVarInt(offer.priceJitter());
            buffer.writeVarInt(offer.surplusThreshold());
        }
    }

    private static ItemMatch readItemMatch(@Nonnull FriendlyByteBuf buffer) {
        return ItemMatchCodec.STREAM_CODEC.decode(buffer);
    }

    private static void writeItemMatch(@Nonnull FriendlyByteBuf buffer, @Nonnull ItemMatch match) {
        ItemMatchCodec.STREAM_CODEC.encode(buffer, match);
    }

}
