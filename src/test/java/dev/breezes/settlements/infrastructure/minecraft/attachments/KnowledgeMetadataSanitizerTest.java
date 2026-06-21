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
                "registry_id", "minecraft:wheat",
                "pos_x", "10",
                "pos_y", "64",
                "pos_z", "-5");

        // Act
        Map<String, String> sanitized = KnowledgeMetadataSanitizer.sanitize(metadata);

        // Assert
        assertEquals(metadata, sanitized);
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

}
