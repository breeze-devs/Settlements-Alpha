package dev.breezes.settlements.infrastructure.rendering.bubbles;

import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
import dev.breezes.settlements.domain.time.ClockTicks;
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
    private static final Comparator<BubbleViewEntry> RENDER_ORDER = Comparator
            .comparingInt(BubbleViewEntry::channelOrder)
            .thenComparing(Comparator.comparingInt(BubbleViewEntry::priority).reversed())
            .thenComparingLong(BubbleViewEntry::createdGameTime)
            .thenComparingLong(BubbleViewEntry::sequenceNumber);

    private final Map<UUID, BubbleViewEntry> bubblesById;

    public BubbleManager() {
        this.bubblesById = new LinkedHashMap<>();
    }

    public Optional<SpeechBubble> getActiveBubble() {
        return this.bubblesById.values().stream()
                .min(RENDER_ORDER)
                .map(BubbleViewEntry::bubble);
    }

    public void tick(double deltaTick) {
        this.bubblesById.values().removeIf(entry -> {
            entry.bubble().tick(deltaTick);
            return entry.bubble().isExpired();
        });
    }

    public void render(@Nonnull RenderParameter parameter) {
        List<BubbleViewEntry> orderedEntries = this.getOrderedEntries();
        for (int i = 0; i < orderedEntries.size(); i++) {
            SpeechBubble bubble = orderedEntries.get(i).bubble();
            parameter.getPoseStack().pushPose();
            parameter.getPoseStack().translate(0.0D, STACKED_BUBBLE_Y_OFFSET * i, 0.0D);
            bubble.render(parameter);
            parameter.getPoseStack().popPose();
        }
    }

    public void removeAllBubbles() {
        this.bubblesById.values().forEach(entry -> entry.bubble().setExpired());
        this.bubblesById.clear();
    }

    public void applySnapshot(@Nonnull List<BubbleEntrySnapshot> entries, long currentGameTime) {
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

            localEntry.getValue().bubble().setExpired();
            iterator.remove();
        }

        for (Map.Entry<UUID, BubbleEntrySnapshot> incomingEntry : incomingById.entrySet()) {
            BubbleEntrySnapshot snapshot = incomingEntry.getValue();
            BubbleViewEntry existing = this.bubblesById.get(snapshot.bubbleId());

            if (existing != null) {
                // Rebuild when contentVersion mismatch, aka the server changed the bubble's content
                // Otherwise preserve the existing instance to avoid resetting per-bubble state
                boolean contentChanged = existing.contentVersion() != snapshot.contentVersion();
                Optional<SpeechBubble> bubble = contentChanged
                        ? this.createBubble(snapshot, currentGameTime)
                        : Optional.of(existing.bubble());
                if (bubble.isEmpty()) {
                    // Keeping the old instance would pair superseded segments with the new contentVersion, so no
                    // later snapshot would retry the rebuild. Dropping the entry matches a failed first build: the
                    // bubble stays absent until a later snapshot builds it successfully.
                    existing.bubble().setExpired();
                    this.bubblesById.remove(snapshot.bubbleId());
                    continue;
                }

                this.bubblesById.put(snapshot.bubbleId(), BubbleViewEntry.builder()
                        .bubble(bubble.get())
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
            ClockTicks remainingLifetime = ClockTicks.of(Math.max(1, entry.expireGameTime() - currentGameTime));
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
        entries.sort(RENDER_ORDER);
        return entries;
    }

    @Builder
    private record BubbleViewEntry(SpeechBubble bubble,
                                   int channelOrder,
                                   int priority,
                                   long createdGameTime,
                                   long sequenceNumber,
                                   int contentVersion) {
    }

}
