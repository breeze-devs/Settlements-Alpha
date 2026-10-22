package dev.breezes.settlements.application.ai.courtship;

import java.util.UUID;

public record CourtshipInvite(
        UUID sessionId,
        UUID presenterId,
        UUID receiverId,
        long createdGameTime,
        long expireGameTime
) {
}
