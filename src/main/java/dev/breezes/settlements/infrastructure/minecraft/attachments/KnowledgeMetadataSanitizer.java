package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
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

    /**
     * Exact-match allowed keys for structured observation metadata.
     * <p>
     * {@code outcome}, {@code reason}, and {@code detail} are allowlisted because they feed the
     * episodic memory (SIS owns phrasing); dropping them on reload would strip failure framing
     * and detail context from resurfaced memories.
     * <p>
     * Structured detail sub-fields ({@code "detail.*"}) are allowed via a prefix check in
     * {@link #isAllowedKey} rather than enumerated here, so adding new detail slots requires
     * no sanitizer change.
     */
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "event_type",
            "event_meta",
            "actor_id",
            "registry_id",
            "outcome",
            "reason",
            "detail"
    );

    static Map<String, String> sanitize(@Nullable Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, String> sanitized = new LinkedHashMap<>(ALLOWED_KEYS.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || !isAllowedKey(key)) {
                continue;
            }

            sanitized.put(key, truncate(value));
        }

        return Map.copyOf(sanitized);
    }

    /**
     * Returns true for any key that should survive the persistence boundary.
     * <p>
     * Structured detail slots ({@code "detail.item"}, {@code "detail.count"}, etc.) are accepted
     * via prefix match so the allowlist does not need updating as new slot names are introduced.
     * All other keys must appear in {@link #ALLOWED_KEYS}; arbitrary keys are still rejected.
     */
    private static boolean isAllowedKey(@Nonnull String key) {
        return ALLOWED_KEYS.contains(key) || key.startsWith(ObservationMetadataKeys.DETAIL_PREFIX);
    }

    private static String truncate(@Nonnull String value) {
        if (value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_VALUE_LENGTH);
    }

}
