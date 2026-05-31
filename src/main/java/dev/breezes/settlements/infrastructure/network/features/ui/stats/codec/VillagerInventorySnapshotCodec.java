package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.domain.inventory.BackpackEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

// TODO: Add round-trip codec test once RegistryFriendlyByteBuf test infrastructure is available (mocking)
public final class VillagerInventorySnapshotCodec {

    private static final int MAX_ITEMS = 512;

    public static VillagerInventorySnapshot read(@Nonnull FriendlyByteBuf buffer) {
        int itemCount = buffer.readVarInt();
        if (itemCount < 0 || itemCount > MAX_ITEMS) {
            throw new IllegalArgumentException("Invalid inventory snapshot itemCount: " + itemCount);
        }

        RegistryFriendlyByteBuf registryBuffer = (RegistryFriendlyByteBuf) buffer;
        List<BackpackEntry> entries = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            ItemStack representative = ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuffer);
            int count = buffer.readVarInt();
            if (count <= 0) {
                throw new IllegalArgumentException("Invalid inventory snapshot entry count: " + count);
            }
            entries.add(new BackpackEntry(representative.copyWithCount(1), count));
        }

        return VillagerInventorySnapshot.builder()
                .entries(entries)
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull VillagerInventorySnapshot snapshot) {
        RegistryFriendlyByteBuf registryBuffer = (RegistryFriendlyByteBuf) buffer;
        List<BackpackEntry> entries = snapshot.entries().stream()
                .filter(entry -> !entry.representative().isEmpty() && entry.count() > 0)
                .toList();
        if (entries.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Inventory snapshot has too many entries: " + entries.size());
        }

        buffer.writeVarInt(entries.size());
        for (BackpackEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuffer, entry.representative().copyWithCount(1));
            buffer.writeVarInt(entry.count());
        }
    }

}
