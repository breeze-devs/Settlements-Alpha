package dev.breezes.settlements.application.ai.planning;

import lombok.Builder;

/**
 * Result of comparing the current Minecraft day-time against the previous plan tick.
 */
@Builder
public record DeltaResult(
        int deltaTicks,
        boolean backwardJump,
        boolean firstTick,
        long rawDelta,
        long previousDayTime
) {
}
