package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Posted when a settlement is removed from the store. Its settlement is the removed record, which the store no longer
 * holds.
 */
public final class SettlementDisbandedEvent extends SettlementLifecycleEvent {

    public SettlementDisbandedEvent(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        super(level, settlement);
    }

}
