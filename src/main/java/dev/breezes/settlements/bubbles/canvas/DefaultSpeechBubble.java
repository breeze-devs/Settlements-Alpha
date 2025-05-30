package dev.breezes.settlements.bubbles.canvas;

import dev.breezes.settlements.bubbles.RenderParameter;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DefaultSpeechBubble implements SpeechBubble {

    private final BubbleBoundaryElement bubbleElement;

    /**
     * Maximum distance (squared) from the bubble that a player can see it
     */
    private final double visibilitySquared;

    private final ITickable tickable;
    @Getter
    private final UUID bubbleId;

    @Builder
    protected DefaultSpeechBubble(@Nonnull UUID uuid,
                                  @Nonnull BubbleBoundaryElement bubbleElement,
                                  double visibilityBlocks,
                                  @Nonnull Ticks lifetime) {
        this.bubbleId = uuid;
        this.bubbleElement = bubbleElement;
        this.visibilitySquared = Math.pow(visibilityBlocks, 2);

        this.tickable = Tickable.of(lifetime);
    }

    @Override
    public void render(@Nonnull RenderParameter parameter) {
        Location location = Location.fromEntity(parameter.getObserver(), true);
        if (location.distance(parameter.getEntity()) > this.visibilitySquared || this.isExpired()) {
            return;
        }

        this.bubbleElement.render(parameter);
    }

    @Override
    public void tick(double deltaTick) {
        this.tickable.tick(deltaTick);
    }

    @Override
    public boolean isExpired() {
        return this.tickable.isComplete();
    }

    @Override
    public void setExpired() {
        this.tickable.forceComplete();
    }

    @Override
    public void reset() {
        this.tickable.reset();
    }

}
