package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.personality.OriginType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerOriginAttachmentCodecTest {

    @Test
    void stateCodec_roundTripsStampedOrigin() {
        // Arrange
        VillagerOriginAttachmentState state = VillagerOriginAttachmentState.of(OriginType.WORLDGEN);

        // Act
        VillagerOriginAttachmentState decoded = decode(encode(state));

        // Assert
        assertTrue(decoded.initialized());
        assertEquals(OriginType.WORLDGEN, decoded.origin());
    }

    @Test
    void stateCodec_roundTripsEmptyState() {
        // Arrange, Act
        VillagerOriginAttachmentState decoded = decode(encode(VillagerOriginAttachmentState.empty()));

        // Assert
        assertFalse(decoded.initialized());
        assertEquals(OriginType.UNKNOWN, decoded.origin());
    }

    @Test
    void stateCodec_decodesUnknownOriginNameAsUnknownFallback() {
        // Arrange — simulates a future OriginType rename/removal; must degrade, not fail the load.
        JsonObject payload = encode(VillagerOriginAttachmentState.of(OriginType.BRED)).getAsJsonObject();
        payload.addProperty("origin", "REMOVED_ORIGIN_TYPE");

        // Act
        VillagerOriginAttachmentState decoded = decode(payload);

        // Assert
        assertTrue(decoded.initialized());
        assertEquals(OriginType.UNKNOWN, decoded.origin());
    }

    private static JsonElement encode(VillagerOriginAttachmentState state) {
        return VillagerOriginAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(Assertions::fail)
                .orElseThrow();
    }

    private static VillagerOriginAttachmentState decode(JsonElement payload) {
        return VillagerOriginAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE, payload)
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst();
    }

    private static final class Assertions {

        private Assertions() {
        }

        private static void fail(String message) {
            org.junit.jupiter.api.Assertions.fail(message);
        }

    }

}
