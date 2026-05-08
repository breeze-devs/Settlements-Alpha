package dev.breezes.settlements.application.ui.bubble;

import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;

import javax.annotation.Nonnull;

@Builder
public record ChannelPolicy(
        int maxActive,
        @Nonnull OverflowPolicy overflowPolicy,
        int renderOrder,
        @Nonnull ClockTicks defaultTtlCap
) {

    public ChannelPolicy {
        if (maxActive <= 0) {
            throw new IllegalArgumentException("maxActive must be > 0");
        }
    }

    public ClockTicks clampTtl(@Nonnull ClockTicks requestedTtl) {
        return ClockTicks.of(Math.min(requestedTtl.getTicks(), this.defaultTtlCap.getTicks()));
    }

    public enum OverflowPolicy {
        /**
         * Always replace the existing active entry (used for single-slot channels).
         */
        REPLACE_EXISTING,
        /**
         * Drop the oldest entry in the channel when capacity is exceeded.
         */
        DROP_OLDEST,
        /**
         * Drop the lowest-priority entry, then oldest, then lowest sequence number.
         */
        DROP_LOWEST_PRIORITY,
        /**
         * Reject the incoming bubble without mutating existing entries.
         */
        REJECT_NEW
    }

}
