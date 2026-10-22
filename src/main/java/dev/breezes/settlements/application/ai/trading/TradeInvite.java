package dev.breezes.settlements.application.ai.trading;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TradeInvite(
        UUID sessionId,
        UUID initiatorId,
        UUID targetId,
        long createdGameTime,
        long expireGameTime
) {
}
