package dev.breezes.settlements.bubbles.canvas;

import dev.breezes.settlements.bubbles.RenderParameter;

import javax.annotation.Nonnull;

public interface SpeechBubble {

    void render(@Nonnull RenderParameter parameter);

    void tick(double deltaTick);

    /**
     * Check if the bubble is expired
     */
    boolean isExpired();

    /**
     * Set the bubble to be expired
     */
    void setExpired();

    /**
     * Reset the bubble's lifetime
     */
    void reset();

}
