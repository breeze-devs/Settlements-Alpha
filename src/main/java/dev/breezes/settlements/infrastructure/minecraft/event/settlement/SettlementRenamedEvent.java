package dev.breezes.settlements.infrastructure.minecraft.event.settlement;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Posted when a stored settlement takes a new name. Its settlement carries the new name.
 */
@Getter
public final class SettlementRenamedEvent extends SettlementLifecycleEvent {

    /**
     * The name the settlement had before.
     */
    private final String previousName;

    /**
     * The player who renamed the settlement, or null when no player did.
     */
    @Nullable
    private final ServerPlayer renamer;

    public SettlementRenamedEvent(@Nonnull ServerLevel level,
                                  @Nonnull Settlement settlement,
                                  @Nonnull String previousName,
                                  @Nullable ServerPlayer renamer) {
        super(level, settlement);
        this.previousName = previousName;
        this.renamer = renamer;
    }

}
