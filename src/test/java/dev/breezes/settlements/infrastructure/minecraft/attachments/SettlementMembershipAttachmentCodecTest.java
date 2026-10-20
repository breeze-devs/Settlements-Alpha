package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.DataResult;
import dev.breezes.settlements.application.settlement.membership.SettlementMembership;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementMembershipAttachmentCodecTest {

    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));

    private static final SettlementMembership MEMBERSHIP = new SettlementMembership(new UUID(0L, 1L), NETHER);

    @Test
    void stateCodec_roundTripsAMembership() {
        // Act
        DataResult<SettlementMembership> decoded = decode(encode(MEMBERSHIP));

        // Assert
        assertEquals(MEMBERSHIP, decoded.getOrThrow());
    }

    @Test
    void stateCodec_failsToRead_aMembershipWithoutItsDimension() {
        // Arrange — no dimension is guessed for it, since settlement ids repeat across dimensions
        CompoundTag stored = encode(MEMBERSHIP);
        stored.remove("dimension");

        // Act
        DataResult<SettlementMembership> decoded = decode(stored);

        // Assert
        assertTrue(decoded.isError());
    }

    @Test
    void stateCodec_failsToRead_aSettlementIdThatIsNotAUuid() {
        // Arrange
        CompoundTag stored = encode(MEMBERSHIP);
        stored.putString("settlement_id", "Ashford");

        // Act
        DataResult<SettlementMembership> decoded = decode(stored);

        // Assert
        assertTrue(decoded.isError());
    }

    private static CompoundTag encode(SettlementMembership membership) {
        return (CompoundTag) SettlementMembershipAttachmentCodec.STATE_CODEC.encodeStart(NbtOps.INSTANCE, membership)
                .getOrThrow(message -> new AssertionError("Expected the membership to encode: " + message));
    }

    private static DataResult<SettlementMembership> decode(Tag stored) {
        return SettlementMembershipAttachmentCodec.STATE_CODEC.parse(NbtOps.INSTANCE, stored);
    }

}
