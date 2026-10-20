package dev.breezes.settlements.application.settlement.lifecycle;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reports changes to a settlement's existence and name, each once the store holds the change.
 * <p>
 * Must be called on the server thread. A listener's failure never reaches the caller, since the change it reports is
 * already stored.
 */
public interface SettlementLifecyclePublisher {

    /**
     * Reports a settlement founded once its founders have joined it.
     *
     * @param level      the level storing the settlement
     * @param settlement the settlement as stored with its founders
     */
    void founded(@Nonnull ServerLevel level, @Nonnull Settlement settlement);

    /**
     * Reports a generated settlement's record, just stored. May be called while a chunk is loading.
     *
     * @param level      the level storing the settlement
     * @param settlement the settlement as stored
     */
    void recorded(@Nonnull ServerLevel level, @Nonnull Settlement settlement);

    /**
     * Reports a stored settlement's new name.
     *
     * @param level        the level storing the settlement
     * @param settlement   the settlement as stored under its new name
     * @param previousName the name it had before
     * @param renamer      the player who renamed it, or null when no player did
     */
    void renamed(@Nonnull ServerLevel level, @Nonnull Settlement settlement, @Nonnull String previousName,
                 @Nullable ServerPlayer renamer);

    /**
     * Reports a settlement removed from the store.
     *
     * @param level      the level that stored the settlement
     * @param settlement the settlement as it was stored when it was removed
     */
    void disbanded(@Nonnull ServerLevel level, @Nonnull Settlement settlement);

}
