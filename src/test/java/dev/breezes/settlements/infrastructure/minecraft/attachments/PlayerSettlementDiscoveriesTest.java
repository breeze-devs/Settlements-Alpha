package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSettlementDiscoveriesTest {

    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    private static final ResourceKey<Level> REMOVED_MOD_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("removed_mod", "realm"));

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID MILLBROOK = new UUID(0L, 2L);

    @Test
    void codec_roundTripsDiscoveries_thatShareAnIdAcrossDimensions() {
        // Arrange: generated settlements in two dimensions can share an id, and each is its own discovery
        PlayerSettlementDiscoveries discoveries = PlayerSettlementDiscoveries.NONE
                .with(OVERWORLD, ASHFORD)
                .with(NETHER, ASHFORD);

        // Act
        PlayerSettlementDiscoveries decoded = decode(encode(discoveries)).getOrThrow();

        // Assert
        assertEquals(discoveries, decoded);
        assertTrue(decoded.contains(OVERWORLD, ASHFORD));
        assertTrue(decoded.contains(NETHER, ASHFORD));
    }

    @Test
    void codec_dropsOnlyAnUnreadableEntry_keepingTheOthers() {
        // Arrange
        CompoundTag stored = encode(PlayerSettlementDiscoveries.NONE
                .with(OVERWORLD, ASHFORD)
                .with(OVERWORLD, MILLBROOK));
        stored.getList("settlements", Tag.TAG_COMPOUND).getCompound(0).putString("settlement_id", "Ashford");

        // Act
        DataResult<PlayerSettlementDiscoveries> decoded = decode(stored);

        // Assert: one corrupted entry must not cost the player every other discovery
        assertTrue(decoded.isSuccess());
        assertEquals(PlayerSettlementDiscoveries.NONE.with(OVERWORLD, MILLBROOK), decoded.getOrThrow());
    }

    @Test
    void pruned_dropsASettlementItsLoadedDimensionNoLongerStores_andKeepsAStoredOne() {
        // Arrange
        PlayerSettlementDiscoveries discoveries = PlayerSettlementDiscoveries.NONE
                .with(OVERWORLD, ASHFORD)
                .with(OVERWORLD, MILLBROOK);

        // Act: Ashford has disbanded
        PlayerSettlementDiscoveries pruned = discoveries.pruned(dimension -> true,
                (dimension, settlementId) -> settlementId.equals(MILLBROOK));

        // Assert
        assertFalse(pruned.contains(OVERWORLD, ASHFORD));
        assertTrue(pruned.contains(OVERWORLD, MILLBROOK));
    }

    @Test
    void pruned_keepsAnEntryWhoseDimensionIsNotLoaded_withoutAskingWhetherItIsStored() {
        // Arrange
        PlayerSettlementDiscoveries discoveries = PlayerSettlementDiscoveries.NONE.with(REMOVED_MOD_DIMENSION, ASHFORD);

        // Act: a dimension with no level has no store to ask
        PlayerSettlementDiscoveries pruned = discoveries.pruned(dimension -> !dimension.equals(REMOVED_MOD_DIMENSION),
                (dimension, settlementId) -> {
                    throw new AssertionError("Asked whether " + dimension.location() + " stores " + settlementId);
                });

        // Assert
        assertTrue(pruned.contains(REMOVED_MOD_DIMENSION, ASHFORD));
    }

    private static CompoundTag encode(PlayerSettlementDiscoveries discoveries) {
        return (CompoundTag) PlayerSettlementDiscoveries.CODEC.encodeStart(NbtOps.INSTANCE, discoveries)
                .getOrThrow(message -> new AssertionError("Expected the discoveries to encode: " + message));
    }

    private static DataResult<PlayerSettlementDiscoveries> decode(Tag stored) {
        return PlayerSettlementDiscoveries.CODEC.parse(NbtOps.INSTANCE, stored);
    }

}
