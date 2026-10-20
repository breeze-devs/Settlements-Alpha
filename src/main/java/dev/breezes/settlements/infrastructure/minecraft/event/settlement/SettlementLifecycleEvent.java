package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

import javax.annotation.Nonnull;

/**
 * A change to a settlement's existence or name, posted on the game bus once the store holds the change.
 * <p>
 * Posted on the server thread only. The change is final: the event cannot be canceled.
 */
@Getter
public abstract class SettlementLifecycleEvent extends Event {

    /**
     * The level storing the settlement, or for a disband, the level that stored it.
     */
    private final ServerLevel level;

    /**
     * The settlement's record as now stored, or for a disband, as it was stored when it was removed.
     */
    private final Settlement settlement;

    protected SettlementLifecycleEvent(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        this.level = level;
        this.settlement = settlement;
    }

}
