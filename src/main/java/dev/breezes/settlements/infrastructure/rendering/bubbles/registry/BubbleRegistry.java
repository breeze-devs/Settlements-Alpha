package dev.breezes.settlements.infrastructure.rendering.bubbles.registry;

import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.SpeechBubble;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.DisplayBubbleRequest;
import dev.breezes.settlements.domain.time.Ticks;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public class BubbleRegistry {

    public static Optional<SpeechBubble> getBubble(@Nonnull DisplayBubbleRequest request) {
        try {
            UUID uuid = request.getBubbleId();
            int visibilityBlocks = request.getVisibilityBlocks();
            int lifetimeTicks = request.getLifetimeTicks();

            switch (request.getBubbleType()) {
                case SHEAR_SHEEP -> {
                    return Optional.of(new ShearSheepSpeechBubble(uuid, visibilityBlocks, Ticks.of(lifetimeTicks)));
                }
                default -> log.warn("Unknown bubble type: {}", request.getBubbleType());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to generate bubble: {}", request.toString(), e);
            return Optional.empty();
        }
    }

}
