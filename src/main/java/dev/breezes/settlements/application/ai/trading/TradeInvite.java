package dev.breezes.settlements.application.ai.trading;

import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.UUID;

@Builder
public record TradeInvite(
        @Nonnull UUID sessionId,
        @Nonnull UUID initiatorId,
        @Nonnull UUID targetId,
        long createdGameTime,
        long expireGameTime
) {
}
