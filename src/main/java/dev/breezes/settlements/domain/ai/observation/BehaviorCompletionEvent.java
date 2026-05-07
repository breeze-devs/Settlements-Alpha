package dev.breezes.settlements.domain.ai.observation;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Emitted when a plan slot finishes execution (regardless of outcome) and added to the
 * {@link ObservationBuffer} to close the behavior → observation → memory feedback loop.
 * <p>
 * {@code details} is a free-form string map for behavior-specific yield data
 * (e.g. items collected, entities bred). Keep values small and serialization-safe.
 * {@code failureReason} is populated only when {@code outcome} is
 * {@link BehaviorOutcome#FAILURE} or {@link BehaviorOutcome#PRECONDITION_FAILED}.
 */
@Builder
public record BehaviorCompletionEvent(
        String behaviorKey,
        BehaviorOutcome outcome,
        long timestampTick,
        Map<String, String> details,
        @Nullable String failureReason
) {
}
