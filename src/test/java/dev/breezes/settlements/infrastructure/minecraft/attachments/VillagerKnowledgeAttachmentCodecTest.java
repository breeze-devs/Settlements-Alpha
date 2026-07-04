package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
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
        KnowledgeEntryState entry = entry(KnowledgeResolution.CONFIRMED);
        VillagerKnowledgeAttachmentState state = VillagerKnowledgeAttachmentState.of(List.of(entry));

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(state));

        // Assert
        assertTrue(decoded.initialized());
        assertEquals(1, decoded.entries().size());
        KnowledgeEntryState decodedEntry = decoded.entries().getFirst();
        assertEquals(entry.originObservationId(), decodedEntry.originObservationId());
        assertEquals(entry.packedPos(), decodedEntry.packedPos());
        assertEquals(KnowledgeResolution.CONFIRMED, decodedEntry.resolution());
    }

    @Test
    void stateCodec_dropsEntryWithUnknownEventTypeOnLoad() {
        // Arrange — type is no longer a persisted field; it is derived from metadata.event_type
        // on the VillagerKnowledgeAttachment.loadInto path. The codec itself still round-trips
        // whatever event_type string is present — the drop happens one layer up.
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(KnowledgeResolution.CONFIRMED))));
        JsonObject metadata = firstEntry(payload).getAsJsonObject("metadata");
        metadata.addProperty("event_type", "REMOVED_EVENT_TYPE");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert — codec decodes the entry regardless; VillagerKnowledgeAttachment is responsible
        // for dropping entries whose event_type no longer resolves (see VillagerKnowledgeAttachmentTest)
        assertEquals(1, decoded.entries().size());
        assertEquals("REMOVED_EVENT_TYPE", decoded.entries().getFirst().metadata().get("event_type"));
    }

    @Test
    void stateCodec_decodesUnknownKnowledgeResolutionAsEmptyKnowledge() {
        // Arrange
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(KnowledgeResolution.UNRESOLVED))));
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
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(KnowledgeResolution.UNRESOLVED))));
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
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(KnowledgeResolution.CONFIRMED))));
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
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(entry(KnowledgeResolution.CONFIRMED))));
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

    @Test
    void entryCodec_omitsHopWhenZero() {
        // Arrange — hop = 0 is the overwhelmingly common (first-hand) case
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().hop(0).build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));

        // Assert
        assertFalse(firstEntry(payload).has("hop"), "hop must be omitted from the wire when 0");
    }

    @Test
    void entryCodec_writesHopWhenNonZero() {
        // Arrange
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().hop(2).build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));

        // Assert
        assertEquals(2, firstEntry(payload).get("hop").getAsInt());
    }

    @Test
    void entryCodec_roundTripsHop() {
        // Arrange
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().hop(3).build();

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(VillagerKnowledgeAttachmentState.of(List.of(state))));

        // Assert
        assertEquals(3, decoded.entries().getFirst().hop());
    }

    @Test
    void entryCodec_omitsAdmittedAtTickWhenEqualToOriginTimestampTick() {
        // Arrange — first-hand entries always have admittedAtTick == originTimestampTick
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder()
                .originTimestampTick(500L)
                .admittedAtTick(500L)
                .build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));

        // Assert
        assertFalse(firstEntry(payload).has("admittedAtTick"),
                "admittedAtTick must be omitted from the wire when equal to originTimestampTick");
    }

    @Test
    void entryCodec_writesAdmittedAtTickWhenDifferentFromOriginTimestampTick() {
        // Arrange — hearsay entries admit later than the origin observation
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder()
                .originTimestampTick(500L)
                .admittedAtTick(650L)
                .build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));

        // Assert
        assertEquals(650L, firstEntry(payload).get("admittedAtTick").getAsLong());
    }

    @Test
    void entryCodec_missingAdmittedAtTickDefaultsToDecodedOriginTimestampTick() {
        // Arrange — simulate a payload with admittedAtTick absent
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder()
                .originTimestampTick(777L)
                .admittedAtTick(777L)
                .build();
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));
        firstEntry(payload).remove("admittedAtTick");

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert — defaults to the DECODED originTimestampTick, not zero
        assertEquals(777L, decoded.entries().getFirst().admittedAtTick());
    }

    @Test
    void entryCodec_omitsPosWhenNull() {
        // Arrange
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().packedPos(null).build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));

        // Assert
        assertFalse(firstEntry(payload).has("pos"), "pos must be omitted from the wire when null");
    }

    @Test
    void entryCodec_roundTripsPosWhenPresent() {
        // Arrange
        long packedPos = 123456789L;
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().packedPos(packedPos).build();

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(VillagerKnowledgeAttachmentState.of(List.of(state))));

        // Assert
        assertEquals(packedPos, decoded.entries().getFirst().packedPos());
    }

    @Test
    void uuidCodec_roundTripsAsIntArrayNotString() {
        // Arrange — B2: UUIDs persist as a 4-int array rather than a 36-char string
        UUID originId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder().originObservationId(originId).build();

        // Act
        JsonElement payload = encode(VillagerKnowledgeAttachmentState.of(List.of(state)));
        JsonElement encodedId = firstEntry(payload).get("originObservationId");
        VillagerKnowledgeAttachmentState decoded = decode(payload);

        // Assert — encoded as a JSON array of 4 ints, not a string
        assertTrue(encodedId.isJsonArray(), "UUID must encode as an int array, not a string");
        assertEquals(4, encodedId.getAsJsonArray().size());
        assertEquals(originId, decoded.entries().getFirst().originObservationId());
    }

    @Test
    void uuidCodec_roundTripsRelatedEntityAndSourceAsIntArrays() {
        // Arrange
        UUID relatedEntity = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID source = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");
        KnowledgeEntryState state = entry(KnowledgeResolution.CONFIRMED).toBuilder()
                .relatedEntity(relatedEntity)
                .source(source)
                .build();

        // Act
        VillagerKnowledgeAttachmentState decoded = decode(encode(VillagerKnowledgeAttachmentState.of(List.of(state))));

        // Assert
        assertEquals(relatedEntity, decoded.entries().getFirst().relatedEntity());
        assertEquals(source, decoded.entries().getFirst().source());
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

    private static KnowledgeEntryState entry(KnowledgeResolution resolution) {
        return KnowledgeEntryState.builder()
                .originObservationId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .originTimestampTick(100L)
                .admittedAtTick(120L)
                .relatedEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .metadata(Map.of("event_type", "RESOURCE_HARVESTED"))
                .packedPos(42L)
                .source(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .hop(1)
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
