package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Detects pending reactive overrides by polling the session registries
 * <p>
 * We use a registry poll here instead of the world event bus cursor since cursors are short-lived.
 * An invitation to trade should survive longer than just the event bus notificiation TTL.
 * <p>
 * Priority order (highest first): courtship invite, then trade invite. Both obligations are roughly
 * equal in weight; courtship is checked first only to give deterministic behavior when both are
 * pending. The losing invite is detected on the next tick.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class OverrideTriggerDetector {

    private final CourtshipSessionRegistry courtshipSessionRegistry;
    private final TradeSessionRegistry tradeSessionRegistry;

    /**
     * Returns the key of the highest-priority pending override for the given villager, or
     * {@code null} when no invite is pending.
     */
    @Nullable
    public BehaviorKey detect(UUID villagerId) {
        if (this.courtshipSessionRegistry.hasInviteFor(villagerId)) {
            return BehaviorKey.COURTSHIP_ACCEPT;
        }
        if (this.tradeSessionRegistry.hasInviteFor(villagerId)) {
            return BehaviorKey.TRADE_ACCEPT;
        }
        return null;
    }

}
