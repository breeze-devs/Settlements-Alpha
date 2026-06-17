package dev.breezes.settlements.infrastructure.minecraft.attachments;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes knowledge metadata before it crosses the villager attachment persistence boundary.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class KnowledgeMetadataSanitizer {

    static final int MAX_VALUE_LENGTH = 128;

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "event_type",
            "event_meta",
            "actor_id",
            "registry_id",
            "pos_x",
            "pos_y",
            "pos_z"
    );

    static Map<String, String> sanitize(@Nullable Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, String> sanitized = new LinkedHashMap<>(ALLOWED_KEYS.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || !ALLOWED_KEYS.contains(key)) {
                continue;
            }

            sanitized.put(key, truncate(value));
        }

        return Map.copyOf(sanitized);
    }

    private static String truncate(@Nonnull String value) {
        if (value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_VALUE_LENGTH);
    }

}
