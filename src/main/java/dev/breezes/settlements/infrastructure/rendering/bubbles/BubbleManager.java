package dev.breezes.settlements.infrastructure.rendering.bubbles;

import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.SpeechBubble;
import dev.breezes.settlements.infrastructure.rendering.bubbles.registry.SegmentComposedSpeechBubble;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@CustomLog
@ClientSide
public class BubbleManager {

    private static final double STACKED_BUBBLE_Y_OFFSET = 0.35D;
    private static final int DEFAULT_VISIBILITY_BLOCKS = 32;

    private final Map<UUID, BubbleViewEntry> bubblesById;
    private long nextLocalSequenceNumber;

    public BubbleManager() {
        this.bubblesById = new LinkedHashMap<>();
        this.nextLocalSequenceNumber = 0L;
    }

    public Optional<SpeechBubble> getActiveBubble() {
        List<BubbleViewEntry> orderedEntries = this.getOrderedEntries();
        if (orderedEntries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(orderedEntries.getFirst().getBubble());
    }

    public void tick(double deltaTick) {
        this.bubblesById.values().removeIf(entry -> {
            entry.getBubble().tick(deltaTick);
            return entry.getBubble().isExpired();
        });
    }

    public void render(@Nonnull RenderParameter parameter) {
        List<BubbleViewEntry> orderedEntries = this.getOrderedEntries();
        for (int i = 0; i < orderedEntries.size(); i++) {
            SpeechBubble bubble = orderedEntries.get(i).getBubble();
            parameter.getPoseStack().pushPose();
            parameter.getPoseStack().translate(0.0D, STACKED_BUBBLE_Y_OFFSET * i, 0.0D);
            bubble.render(parameter);
            parameter.getPoseStack().popPose();
        }
    }

    public void removeAllBubbles() {
        this.bubblesById.values().forEach(entry -> entry.getBubble().setExpired());
        this.bubblesById.clear();
    }

    public void applySnapshot(@Nonnull List<BubbleEntrySnapshot> entries, long currentGameTime) {
        log.info("Bubble applySnapshot: incoming={} existing={} incomingSources={}",
                entries.size(), this.bubblesById.size(),
                entries.stream().map(BubbleEntrySnapshot::sourceType).toList());

        Map<UUID, BubbleEntrySnapshot> incomingById = new LinkedHashMap<>();
        for (BubbleEntrySnapshot entry : entries) {
            incomingById.put(entry.bubbleId(), entry);
        }

        Iterator<Map.Entry<UUID, BubbleViewEntry>> iterator = this.bubblesById.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BubbleViewEntry> localEntry = iterator.next();
            if (incomingById.containsKey(localEntry.getKey())) {
                continue;
            }

            localEntry.getValue().getBubble().setExpired();
            iterator.remove();
        }

        for (Map.Entry<UUID, BubbleEntrySnapshot> incomingEntry : incomingById.entrySet()) {
            BubbleEntrySnapshot snapshot = incomingEntry.getValue();
            BubbleViewEntry existing = this.bubblesById.get(snapshot.bubbleId());

            if (existing != null) {
                // A contentVersion mismatch means the server changed the bubble's segments.
                // Only in that case do we rebuild; otherwise we preserve the existing instance
                // to avoid resetting per-bubble animation state (e.g. sprite frame clocks).
                boolean contentChanged = existing.getContentVersion() != snapshot.contentVersion();
                SpeechBubble bubble = contentChanged
                        ? this.createBubble(snapshot, currentGameTime).orElse(existing.getBubble())
                        : existing.getBubble();

                this.bubblesById.put(snapshot.bubbleId(), BubbleViewEntry.builder()
                        .bubbleId(snapshot.bubbleId())
                        .bubble(bubble)
                        .contentVersion(snapshot.contentVersion())
                        .channelOrder(snapshot.channel().ordinal())
                        .priority(snapshot.priority())
                        .createdGameTime(snapshot.createdGameTime())
                        .sequenceNumber(snapshot.sequenceNumber())
                        .build());
                continue;
            }

            this.createBubble(snapshot, currentGameTime)
                    .ifPresent(bubble -> this.bubblesById.put(snapshot.bubbleId(), BubbleViewEntry.builder()
                            .bubbleId(snapshot.bubbleId())
                            .bubble(bubble)
                            .contentVersion(snapshot.contentVersion())
                            .channelOrder(snapshot.channel().ordinal())
                            .priority(snapshot.priority())
                            .createdGameTime(snapshot.createdGameTime())
                            .sequenceNumber(snapshot.sequenceNumber())
                            .build()));
        }
    }

    protected Optional<SpeechBubble> createBubble(@Nonnull BubbleEntrySnapshot entry, long currentGameTime) {
        try {
            Ticks remainingLifetime = Ticks.of(Math.max(1, entry.expireGameTime() - currentGameTime));
            return Optional.of(new SegmentComposedSpeechBubble(
                    entry.bubbleId(),
                    entry.segments(),
                    DEFAULT_VISIBILITY_BLOCKS,
                    remainingLifetime));
        } catch (Exception e) {
            log.error("Failed to generate bubble from snapshot: {}", entry, e);
            return Optional.empty();
        }
    }

    private List<BubbleViewEntry> getOrderedEntries() {
        List<BubbleViewEntry> entries = new ArrayList<>(this.bubblesById.values());
        entries.sort(buildComparator());
        return entries;
    }

    private static Comparator<BubbleViewEntry> buildComparator() {
        return Comparator.comparingInt(BubbleViewEntry::getChannelOrder)
                .thenComparing((left, right) -> Integer.compare(right.getPriority(), left.getPriority()))
                .thenComparingLong(BubbleViewEntry::getCreatedGameTime)
                .thenComparingLong(BubbleViewEntry::getSequenceNumber);
    }

    @Builder
    @Getter
    private static class BubbleViewEntry {

        private UUID bubbleId;
        private SpeechBubble bubble;
        private int channelOrder;
        private int priority;
        private long createdGameTime;
        private long sequenceNumber;
        private int contentVersion;

    }

}
