package dev.breezes.settlements.infrastructure.rendering.bubbles.registry;

import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleBoundaryElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleHorizontalCompositeElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.BubbleInnerElement;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.DefaultSpeechBubble;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public final class SegmentComposedSpeechBubble extends DefaultSpeechBubble {

    public SegmentComposedSpeechBubble(@Nonnull UUID uuid,
                                       @Nonnull List<BubbleSegment> segments,
                                       double visibilityBlocks,
                                       @Nonnull ClockTicks lifetime) {
        super(uuid, buildBoundary(segments), visibilityBlocks, lifetime);
    }

    private static BubbleBoundaryElement buildBoundary(@Nonnull List<BubbleSegment> segments) {
        List<BubbleInnerElement> children = segments.stream()
                .map(SegmentRenderer::toElement)
                .toList();
        return BubbleBoundaryElement.builder()
                .innerElement(BubbleHorizontalCompositeElement.builder()
                        .children(children)
                        .gap(4)
                        .build())
                .opacity(1.0F)
                .build();
    }

}
