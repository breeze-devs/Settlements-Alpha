package dev.breezes.settlements.application.ui.bubble;

import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Replicated bubble snapshot entry sent from the authoritative server runtime to clients.
 */
@Builder
public record BubbleEntrySnapshot(
        @Nonnull UUID bubbleId,
        @Nonnull BubbleChannel channel,
        @Nullable String ownerKey,
        int priority,
        long expireGameTime,
        long createdGameTime,
        long sequenceNumber,
        int contentVersion,
        @Nonnull String sourceType,
        @Nonnull List<BubbleSegment> segments
) {

    public BubbleEntrySnapshot {
        segments = List.copyOf(segments);
        if (createdGameTime < 0) {
            throw new IllegalArgumentException("createdGameTime must be >= 0");
        }
        if (expireGameTime <= createdGameTime) {
            throw new IllegalArgumentException("expireGameTime must be > createdGameTime");
        }
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("sequenceNumber must be >= 0");
        }
        if (ownerKey != null && ownerKey.isBlank()) {
            throw new IllegalArgumentException("ownerKey must not be blank");
        }
        if (sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType must not be blank");
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }
    }

    public static BubbleEntrySnapshot fromEntry(@Nonnull BubbleEntry entry) {
        return BubbleEntrySnapshot.builder()
                .bubbleId(entry.bubbleId())
                .channel(entry.channel())
                .ownerKey(entry.ownerKey())
                .priority(entry.message().getPriority())
                .expireGameTime(entry.expireGameTime())
                .createdGameTime(entry.createdGameTime())
                .sequenceNumber(entry.sequenceNumber())
                .contentVersion(entry.contentVersion())
                .sourceType(entry.message().getSourceType())
                .segments(entry.message().getSegments())
                .build();
    }

}
