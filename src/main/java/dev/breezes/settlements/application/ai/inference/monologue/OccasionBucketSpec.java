package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import lombok.Builder;
import lombok.Getter;

/**
 * Requested line count for one occasion-specific monologue bucket.
 */
@Builder
@Getter
public final class OccasionBucketSpec {

    private final Occasion occasion;
    private final int lineCount;

}
