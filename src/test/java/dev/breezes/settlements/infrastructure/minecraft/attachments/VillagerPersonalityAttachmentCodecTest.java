package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.personality.PersonalityStatus;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerPersonalityAttachmentCodecTest {

    @Test
    void stateCodec_roundTripsReadyPersonality() {
        // Arrange
        VillagerPersonality personality = VillagerPersonality.ready(List.of("gruff", "loyal"), "A gruff but loyal blacksmith.", "terse");

        // Act
        VillagerPersonality decoded = decode(encode(personality));

        // Assert
        assertEquals(PersonalityStatus.READY, decoded.status());
        assertEquals(List.of("gruff", "loyal"), decoded.adjectives());
        assertEquals("A gruff but loyal blacksmith.", decoded.characterSketch());
        assertEquals("terse", decoded.speechStyle());
    }

    @Test
    void stateCodec_decodesUnknownStatusNameAsPendingFallback() {
        // Arrange — simulates a future PersonalityStatus rename/removal; must degrade, not fail the load.
        JsonObject payload = encode(VillagerPersonality.ready(List.of("brave"), "characterSketch", "style")).getAsJsonObject();
        payload.addProperty("status", "REMOVED_STATUS");

        // Act
        VillagerPersonality decoded = decode(payload);

        // Assert
        assertEquals(PersonalityStatus.PENDING, decoded.status());
    }

    @Test
    void stateCodec_decodesEmptyObjectAsPendingDefaults() {
        // Arrange — every field is optionalFieldOf-defaulted, so an absent attachment (or one from
        // before this wave shipped) must decode to pending()-equivalent content.
        JsonObject payload = new JsonObject();

        // Act
        VillagerPersonality decoded = decode(payload);

        // Assert
        assertEquals(PersonalityStatus.PENDING, decoded.status());
        assertTrue(decoded.adjectives().isEmpty());
        assertEquals("", decoded.characterSketch());
        assertEquals("", decoded.speechStyle());
    }

    private static JsonElement encode(VillagerPersonality personality) {
        return VillagerPersonalityAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, personality)
                .resultOrPartial(Assertions::fail)
                .orElseThrow();
    }

    private static VillagerPersonality decode(JsonElement payload) {
        return VillagerPersonalityAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE, payload)
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
