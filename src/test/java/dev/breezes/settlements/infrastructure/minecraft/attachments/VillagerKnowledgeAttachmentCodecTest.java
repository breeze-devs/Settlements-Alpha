package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerKnowledgeAttachmentCodecTest {

    @Test
    void stateCodec_roundTripsPersistedKnowledgeState() {
        // Arrange
        KnowledgeEntryState entry = entry(ObservationType.RESOURCE, KnowledgeResolution.CONFIRMED);
        VillagerKnowledgeAttachmentState state = VillagerKnowledgeAttachmentState.of(List.of(entry));

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(state));

        // Assert
        assertTrue(decoded.initialized());
        assertEquals(1, decoded.entries().size());
        KnowledgeEntryState decodedEntry = decoded.entries().getFirst();
        assertEquals(entry.originObservationId(), decodedEntry.originObservationId());
        assertEquals(entry.content(), decodedEntry.content());
        assertEquals(ObservationType.RESOURCE, decodedEntry.type());
        assertEquals(KnowledgeResolution.CONFIRMED, decodedEntry.resolution());
    }

    @Test
    void stateCodec_decodesUnknownObservationTypeAsEmptyKnowledge() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(ObservationType.RESOURCE, KnowledgeResolution.CONFIRMED))));
        firstEntry(payload).addProperty("type", "REMOVED_OBSERVATION_TYPE");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert
        assertTrue(decoded.initialized());
        assertTrue(decoded.entries().isEmpty());
    }

    @Test
    void stateCodec_decodesUnknownKnowledgeResolutionAsEmptyKnowledge() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(ObservationType.SOCIAL, KnowledgeResolution.UNRESOLVED))));
        firstEntry(payload).addProperty("resolution", "REMOVED_RESOLUTION");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert
        assertTrue(decoded.initialized());
        assertTrue(decoded.entries().isEmpty());
    }

    @Test
    void stateCodec_roundTripsMissingResolutionAsNull() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(ObservationType.THREAT, KnowledgeResolution.UNRESOLVED))));
        firstEntry(payload).remove("resolution");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert
        assertEquals(1, decoded.entries().size());
        assertNull(decoded.entries().getFirst().resolution());
    }

    @Test
    void stateCodec_dropsUnknownMetadataKeysWhenDecoding() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(ObservationType.RESOURCE, KnowledgeResolution.CONFIRMED))));
        JsonObject metadata = firstEntry(payload).getAsJsonObject("metadata");
        metadata.addProperty("event_type", "RESOURCE_HARVESTED");
        metadata.addProperty("unbounded_payload", "should not persist");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert
        Map<String, String> decodedMetadata = decoded.entries().getFirst().metadata();
        assertEquals(Map.of("event_type", "RESOURCE_HARVESTED"), decodedMetadata);
    }

    @Test
    void stateCodec_truncatesLongMetadataValuesWhenDecoding() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(ObservationType.RESOURCE, KnowledgeResolution.CONFIRMED))));
        String oversizedValue = "x".repeat(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH + 1);
        firstEntry(payload).getAsJsonObject("metadata").addProperty("event_meta", oversizedValue);

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert
        String decodedValue = decoded.entries().getFirst().metadata().get("event_meta");
        assertEquals(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH, decodedValue.length());
        assertEquals("x".repeat(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH), decodedValue);
    }

    @Test
    void stateCodec_roundTripsEmptyState() {
        // Arrange, Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(VillagerKnowledgeAttachmentState.empty()));

        // Assert
        assertFalse(decoded.initialized());
        assertTrue(decoded.entries().isEmpty());
    }

    private static JsonElement encode(VillagerKnowledgeAttachmentState state) {
        return VillagerKnowledgeAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(Assertions::fail)
                .orElseThrow();
    }

    private static VillagerKnowledgeAttachmentState decode(JsonElement payload) {
        return VillagerKnowledgeAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE, payload)
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst();
    }

    private static JsonObject firstEntry(JsonElement payload) {
        JsonObject object = payload.getAsJsonObject();
        JsonArray entries = object.getAsJsonArray("entries");
        return entries.get(0).getAsJsonObject();
    }

    private static KnowledgeEntryState entry(ObservationType type, KnowledgeResolution resolution) {
        return KnowledgeEntryState.builder()
                .originObservationId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .content("ripe wheat near the farm")
                .type(type)
                .originTimestampTick(100L)
                .admittedAtTick(120L)
                .relatedEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .metadata(Map.of("source", "test"))
                .source(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .hop(1)
                .weight(2.5F)
                .originalWeight(3.0F)
                .resolution(resolution)
                .corroborationCount(2)
                .investigationAttempts(1)
                .nextEligibleTick(200L)
                .build();
    }

    private static final class Assertions {

        private Assertions() {
        }

        private static void fail(String message) {
            org.junit.jupiter.api.Assertions.fail(message);
        }

    }

}
