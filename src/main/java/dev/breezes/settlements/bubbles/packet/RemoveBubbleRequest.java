package dev.breezes.settlements.bubbles.packet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
public class RemoveBubbleRequest {

    /**
     * Entity networking ID for the (Settlements) entity to which the bubble is attached
     */
    private int entityId;

    private final UUID bubbleId;

    @Builder.Default
    private final Map<String, String> extraData = Collections.emptyMap();

}
