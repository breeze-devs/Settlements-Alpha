package dev.breezes.settlements.application.settlement.persistence;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Receives each change {@link SettlementStore} makes to a level's settlements.
 * <p>
 * A listener must not change the store.
 */
public interface SettlementStoreListener {

    /**
     * Called once for each settlement the store adds, writes, or disbands, after the store holds the change.
     * At least one of before and after is present.
     *
     * @param level  the level storing the settlement
     * @param before the settlement as it was stored, or null when it was just added
     * @param after  the settlement as it is now stored, or null when it was just disbanded
     */
    void onSettlementChanged(@Nonnull ServerLevel level, @Nullable Settlement before, @Nullable Settlement after);

}
