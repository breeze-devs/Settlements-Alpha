package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.ui.stats.model.DemandDisplayEntry;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class VillagerDemandDisplaySnapshotCodecTest {

    @Test
    void roundtrip_preservesEntryFields() {
        VillagerDemandDisplaySnapshot input = new VillagerDemandDisplaySnapshot(List.of(
                DemandDisplayEntry.builder()
                        .id("bread_signal")
                        .match(new ItemMatch.ItemRef(ResourceLocation.withDefaultNamespace("bread")))
                        .desiredMinCount(16)
                        .basePricePerUnit(4)
                        .basePriority(2)
                        .activeDemand(ActiveDemand.builder()
                                .match(new ItemMatch.ItemRef(ResourceLocation.withDefaultNamespace("bread")))
                                .desiredCount(12)
                                .priority(7)
                                .basePricePerUnit(4)
                                .origin(ActiveDemand.Origin.SIGNAL)
                                .build())
                        .build(),
                DemandDisplayEntry.builder()
                        .id("planks_baseline")
                        .match(new ItemMatch.TagRef(ItemTags.create(ResourceLocation.withDefaultNamespace("planks"))))
                        .desiredMinCount(32)
                        .basePricePerUnit(9)
                        .basePriority(5)
                        .build()
        ));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        // Arrange, Act, Assert stays explicit here because codec failures usually show up as field drift.
        VillagerDemandDisplaySnapshotCodec.write(buffer, input);
        VillagerDemandDisplaySnapshot decoded = VillagerDemandDisplaySnapshotCodec.read(buffer);

        Assertions.assertEquals(input, decoded);
    }

    @Test
    void constructor_copiesEntriesDefensively() {
        List<DemandDisplayEntry> mutableEntries = new ArrayList<>();
        mutableEntries.add(DemandDisplayEntry.builder()
                .id("apple_stock")
                .match(new ItemMatch.ItemRef(ResourceLocation.withDefaultNamespace("apple")))
                .desiredMinCount(2)
                .basePricePerUnit(4)
                .basePriority(1)
                .build());

        VillagerDemandDisplaySnapshot snapshot = new VillagerDemandDisplaySnapshot(mutableEntries);

        mutableEntries.clear();

        Assertions.assertEquals(1, snapshot.entries().size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.entries().add(null));
    }

}
