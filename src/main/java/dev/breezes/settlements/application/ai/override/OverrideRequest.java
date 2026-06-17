package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import lombok.Builder;
import lombok.Getter;

/**
 * The result of an {@link OverridePolicy} evaluation: a request to install a specific
 * behavior as the override slot.
 * <p>
 * Keeping the request as an explicit value object (rather than a raw {@link BehaviorKey})
 * lets policies attach context that the override launcher can act on — such as which tip
 * was selected — without the launcher needing to re-run the selection logic.
 */
@Builder
@Getter
public final class OverrideRequest {

    /**
     * The behavior to install in the override slot.
     */
    private final BehaviorKey behaviorKey;

}
