package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * Posted when the record of a settlement generated with its structure is stored.
 * <p>
 * It can be posted while a chunk is loading, from inside ChunkEvent.Load. A listener must therefore not load chunks,
 * directly or through a lookup that can, such as a structure lookup; work that might must be deferred, for example to
 * the end of the server tick.
 */
public final class SettlementRecordedEvent extends SettlementLifecycleEvent {

    public SettlementRecordedEvent(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        super(level, settlement);
    }

}
