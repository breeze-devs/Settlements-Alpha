package dev.breezes.settlements.bubbles;

import dev.breezes.settlements.bubbles.canvas.SpeechBubble;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

@Getter
@CustomLog
public class BubbleManager {

    private final PriorityQueue<BubblePriorityEntry> bubbleMaxHeap;

    public BubbleManager() {
        this.bubbleMaxHeap = new PriorityQueue<>(Comparator.comparingInt(BubblePriorityEntry::getPriority).reversed());
    }

    public Optional<SpeechBubble> getActiveBubble() {
        if (this.bubbleMaxHeap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.bubbleMaxHeap.peek().getBubble());
    }

    public void tick(double deltaTick) {
        bubbleMaxHeap.removeIf(entry -> {
            entry.getBubble().tick(deltaTick);
            return entry.getBubble().isExpired();
        });
    }

    public void render(@Nonnull RenderParameter parameter) {
        // Only render the active bubble
        this.getActiveBubble().ifPresent(bubble -> bubble.render(parameter));
    }

    public void addBubble(@Nonnull UUID bubbleId, @Nonnull SpeechBubble bubble, int priority) {
        BubblePriorityEntry entry = BubblePriorityEntry.builder()
                .priority(priority)
                .bubbleId(bubbleId)
                .bubble(bubble)
                .build();
        this.bubbleMaxHeap.add(entry);
    }

    public void removeBubble(@Nonnull UUID bubbleId) {
        this.bubbleMaxHeap.stream()
                .filter(entry -> entry.getBubbleId().equals(bubbleId))
                .findAny()
                .ifPresent(entry -> {
                    entry.getBubble().setExpired();
                    this.bubbleMaxHeap.remove(entry);
                });
    }

    public void removeAllBubbles() {
        this.bubbleMaxHeap.forEach(entry -> entry.getBubble().setExpired());
        this.bubbleMaxHeap.clear();
    }

    @Builder
    @Getter
    private static class BubblePriorityEntry {

        private int priority;
        private UUID bubbleId;
        private SpeechBubble bubble;

    }

}
