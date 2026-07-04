package dev.breezes.settlements.infrastructure.minecraft.attachments;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeMetadataSanitizerTest {

    @Test
    void sanitize_keepsKnownMetadataKeys() {
        // Arrange
        Map<String, String> metadata = Map.of(
                "event_type", "RESOURCE_HARVESTED",
                "event_meta", "settlements:farmer/harvest",
                "actor_id", "00000000-0000-0000-0000-000000000001",
                "registry_id", "minecraft:wheat");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(metadata, sanitized);
    }

    @Test
    void sanitize_dropsPositionKeys() {
        // Arrange — pos_* was replaced by the typed packedPos field (B1); metadata no longer
        // carries position, so any residual pos_* key must be rejected like any other unknown key
        Map<String, String> metadata = Map.of(
                "event_type", "RESOURCE_HARVESTED",
                "pos_x", "10",
                "pos_y", "64",
                "pos_z", "-5");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(Map.of("event_type", "RESOURCE_HARVESTED"), sanitized);
    }

    @Test
    void sanitize_dropsUnknownKeys() {
        // Arrange
        Map<String, String> metadata = Map.of(
                "event_type", "TRADE_COMPLETED",
                "freeform_payload", "large arbitrary text");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(Map.of("event_type", "TRADE_COMPLETED"), sanitized);
    }

    @Test
    void sanitize_truncatesOversizedValues() {
        // Arrange
        String oversizedValue = "a".repeat(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH + 50);

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(Map.of("event_meta", oversizedValue));

        // Assert
        assertEquals(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH, sanitized.get("event_meta").length());
        assertEquals("a".repeat(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH), sanitized.get("event_meta"));
    }

    @Test
    void sanitize_dropsNullValues() {
        // Arrange
        Map<String, String> metadata = new HashMap<>();
        metadata.put("event_type", "RESOURCE");
        metadata.put("event_meta", null);

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(Map.of("event_type", "RESOURCE"), sanitized);
    }

    @Test
    void sanitize_dropsNullKeys() {
        // Arrange
        Map<String, String> metadata = new HashMap<>();
        metadata.put("event_type", "RESOURCE");
        metadata.put(null, "malformed");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(Map.of("event_type", "RESOURCE"), sanitized);
    }

    @Test
    void sanitize_returnsEmptyMapForNullInput() {
        // Arrange, Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(null);

        // Assert
        assertTrue(sanitized.isEmpty());
    }

    @Test
    void sanitize_returnsImmutableMap() {
        // Arrange
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(Map.of("event_type", "SOCIAL"));

        // Act, Assert
        assertFalse(sanitized.isEmpty());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> sanitized.put("event_meta", "mutated"));
    }

    @Test
    void sanitize_preservesOutcomeKey() {
        // Arrange — outcome feeds LLM failure framing; must survive the persistence boundary
        Map<String, String> metadata = Map.of(
                "event_type", "TRADE_COMPLETED",
                "outcome", "FAILURE");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals("FAILURE", sanitized.get("outcome"));
    }

    @Test
    void sanitize_preservesReasonKey() {
        // Arrange — reason context for failure entries must survive the persistence boundary
        Map<String, String> metadata = Map.of(
                "event_type", "TRADE_COMPLETED",
                "reason", "haggling fell through");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals("haggling fell through", sanitized.get("reason"));
    }

    @Test
    void sanitize_preservesFlatDetailKey() {
        // Arrange — flat detail string is a stable key used by existing deeds
        Map<String, String> metadata = Map.of(
                "event_type", "RESOURCE_HARVESTED",
                "detail", "3 melons");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals("3 melons", sanitized.get("detail"));
    }

    @Test
    void sanitize_preservesDetailPrefixedKeys() {
        // Arrange — structured detail sub-fields (detail.*) for LLM phrasing
        Map<String, String> metadata = Map.of(
                "event_type", "RESOURCE_HARVESTED",
                "detail.item", "melons",
                "detail.count", "3");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert — both slots survive
        assertEquals("melons", sanitized.get("detail.item"));
        assertEquals("3", sanitized.get("detail.count"));
    }

    @Test
    void sanitize_detailPrefixedKeysAreTruncated() {
        // Arrange — oversized value in a structured detail slot
        String oversizedValue = "x".repeat(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH + 10);
        Map<String, String> metadata = Map.of(
                "event_type", "TRADE_COMPLETED",
                "detail.item", oversizedValue);

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert — truncation still applies to detail.* values
        assertEquals(KnowledgeMetadataSanitizer.MAX_VALUE_LENGTH, sanitized.get("detail.item").length());
    }

    @Test
    void sanitize_arbitraryKeyIsStillRejected() {
        // Arrange — an unknown key that is not in the allowed set and is not a detail.* prefix
        Map<String, String> metadata = Map.of(
                "event_type", "RESOURCE_HARVESTED",
                "internal_debug_flag", "true");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert — the allowlist still rejects arbitrary keys; only detail.* prefix is special-cased
        assertFalse(sanitized.containsKey("internal_debug_flag"));
    }

}
