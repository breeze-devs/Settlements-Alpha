package dev.breezes.settlements.application.ui.bubble;

import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@Builder
public record BubbleEntry(
        @Nonnull UUID bubbleId,
        @Nonnull BubbleChannel channel,
        @Nullable String ownerKey,
        @Nonnull BubbleMessage message,
        long createdGameTime,
        long expireGameTime,
        long sequenceNumber,
        int contentVersion
) {

    public BubbleEntry {
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
    }

    public boolean isExpiredAt(long gameTime) {
        return gameTime >= this.expireGameTime;
    }

    public BubbleEntry withMessage(@Nonnull BubbleMessage newMessage,
                                   long newCreatedGameTime,
                                   long newExpireGameTime) {
        return BubbleEntry.builder()
                .bubbleId(this.bubbleId)
                .channel(this.channel)
                .ownerKey(this.ownerKey)
                .message(newMessage)
                .createdGameTime(newCreatedGameTime)
                .expireGameTime(newExpireGameTime)
                .sequenceNumber(this.sequenceNumber)
                // Incrementing here signals to the client that segments changed without
                // requiring it to perform a segment-by-segment equality check.
                .contentVersion(this.contentVersion + 1)
                .build();
    }

}
