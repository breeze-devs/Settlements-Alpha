package dev.breezes.settlements.bubbles.packet;

import dev.breezes.settlements.bubbles.registry.BubbleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
public class DisplayBubbleRequest {

    /**
     * Entity networking ID for the (Settlements) entity to which the bubble is attached
     */
    private int entityId;

    /*
     * Bubble configurations
     */
    private final BubbleType bubbleType;
    private final UUID bubbleId;
    private final int visibilityBlocks;
    private final int lifetimeTicks;

    @Builder.Default
    private final Map<String, String> extraData = Collections.emptyMap();

}
