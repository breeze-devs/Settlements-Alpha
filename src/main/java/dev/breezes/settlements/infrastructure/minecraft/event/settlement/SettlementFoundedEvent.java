package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Posted when a settlement is founded at a bell, once its founders have joined it.
 */
public final class SettlementFoundedEvent extends SettlementLifecycleEvent {

    public SettlementFoundedEvent(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        super(level, settlement);
    }

}
