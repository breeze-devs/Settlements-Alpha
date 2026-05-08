package dev.breezes.settlements.application.ui.bubble;

import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Immutable payload describing a single bubble to display for a villager.
 */
@Builder
@Getter
public final class BubbleMessage {

    /**
     * Relative importance of the message within the same render channel.
     * <p>
     * Higher values represent higher priority. The default is 0.
     */
    @Builder.Default
    private final int priority = 0;

    /**
     * How long the bubble should remain visible before expiring.
     */
    private final ClockTicks ttl;

    /**
     * Stable identifier describing the subsystem or feature that produced the bubble.
     */
    private final String sourceType;

    /**
     * Bubble content described as semantic render primitives rather than pre-baked templates.
     * The list is copied defensively so later mutation at the call site cannot desynchronize
     * authoritative state from the network snapshot derived from it.
     */
    private final List<BubbleSegment> segments;

    public BubbleMessage(int priority,
                         @Nonnull ClockTicks ttl,
                         @Nonnull String sourceType,
                         @Nonnull List<BubbleSegment> segments) {
        this.priority = priority;
        this.ttl = ttl;

        if (sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        this.sourceType = sourceType;

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
        this.segments = List.copyOf(segments);
    }

}
