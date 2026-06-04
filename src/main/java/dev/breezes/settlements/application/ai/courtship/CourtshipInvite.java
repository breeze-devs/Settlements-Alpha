package dev.breezes.settlements.application.ai.courtship;

import javax.annotation.Nonnull;
import java.util.UUID;

public record CourtshipInvite(
        @Nonnull UUID sessionId,
        @Nonnull UUID presenterId,
        @Nonnull UUID receiverId,
        long createdGameTime,
        long expireGameTime
) {
}
