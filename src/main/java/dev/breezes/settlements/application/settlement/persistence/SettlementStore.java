package dev.breezes.settlements.application.settlement.persistence;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.world.ChunkPosition;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The persistent settlements of each dimension.
 * <p>
 * Within a level, no two settlements claim the same chunk or list the same member, and none has an id the level
 * tombstoned.
 * <p>
 * Every add that stores a settlement, update that writes one, and disband that deletes one is announced to each bound
 * {@link SettlementStoreListener} before the call returns, on the calling thread. A listener that throws is logged,
 * and its exception reaches neither the caller nor the other listeners.
 */
public interface SettlementStore {

    Optional<Settlement> find(@Nonnull ServerLevel level, @Nonnull UUID settlementId);

    /**
     * Returns the level's settlement claiming the given chunk.
     */
    Optional<Settlement> findClaiming(@Nonnull ServerLevel level, @Nonnull ChunkPosition chunk);

    /**
     * Returns the level's settlement whose roster lists the given member.
     */
    Optional<Settlement> findListing(@Nonnull ServerLevel level, @Nonnull UUID memberId);

    /**
     * Adds a settlement to the level with its claim clipped to the chunks no other settlement claims,
     * keeping only the part still joined to its center chunk by edges.
     *
     * @return the settlement as stored, or empty when the level already stores a settlement with its id, has
     * tombstoned its id, or stores another settlement claiming its center chunk; nothing is stored when empty
     * @throws IllegalArgumentException when the settlement would be stored but lists a member another settlement in
     *                                  the level lists; nothing is stored then
     */
    Optional<Settlement> add(@Nonnull ServerLevel level, @Nonnull Settlement settlement);

    /**
     * Replaces the level's settlement with the given id by the result of the operation applied to it.
     * The settlement keeps its place among the level's settlements.
     * <p>
     * Nothing is written when the operation returns the same settlement it was given.
     * Any other result is written, including an equal copy, so an operation that changes nothing returns its argument.
     *
     * @param operation applied to the stored settlement once; it must not change the store
     * @return the settlement as now stored, or empty when the level stores no settlement with the given id
     * @throws IllegalArgumentException when the result has another id or another claim, or lists a member another
     *                                  settlement in the level lists; nothing is written then
     */
    Optional<Settlement> update(@Nonnull ServerLevel level, @Nonnull UUID settlementId,
                                @Nonnull UnaryOperator<Settlement> operation);

    /**
     * Deletes the level's settlement with the given id if exists, tombstoning it in the level.
     * The deletion and the tombstone are saved together.
     *
     * @return the settlement as it was stored, or empty when the level has no settlement with the id
     */
    Optional<Settlement> disband(@Nonnull ServerLevel level, @Nonnull UUID settlementId);

    /**
     * Returns whether a generated settlement with the given id was disbanded from the level.
     */
    boolean isTombstoned(@Nonnull ServerLevel level, @Nonnull UUID settlementId);

    /**
     * Returns a snapshot of the level's settlements; later changes to the store do not affect the returned list.
     */
    List<Settlement> all(@Nonnull ServerLevel level);

}
