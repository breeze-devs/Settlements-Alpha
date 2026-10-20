package dev.breezes.settlements.infrastructure.minecraft.persistence;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.settlement.model.SettlementRosterEntry;
import dev.breezes.settlements.domain.world.ChunkPosition;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.CustomLog;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The settlements of one dimension, saved with that level's data.
 * <p>
 * Every level has its own store. Settlements are immutable, so the store's own mutators are the only way its content
 * changes, and each change marks it dirty.
 * <p>
 * No two stored settlements share a chunk or list the same member, and no stored settlement has a tombstoned id.
 */
@CustomLog
public final class SettlementSavedData extends SavedData {

    private static final String DATA_NAME = "settlements_settlements";

    private final Map<UUID, Settlement> settlementsById;

    /**
     * The ids of the generated settlements disbanded from this level, in the order they disbanded.
     */
    // Saved with the settlements rather than marked on the structure start that still carries the settlement: the
    // start's chunk saves at other moments than this data, so a crash could keep the deletion without the mark, and
    // that chunk need not be loaded when the settlement disbands
    private final Set<UUID> tombstones = new LinkedHashSet<>();

    /**
     * The id of the settlement claiming each chunk, keyed by the chunk's packed position.
     * Derived from the stored claims, so it is rebuilt on load and never saved.
     */
    private final Long2ObjectOpenHashMap<UUID> settlementIdsByChunk = new Long2ObjectOpenHashMap<>();

    /**
     * The id of the settlement listing each member.
     * Derived from the stored rosters, so it is rebuilt on load and never saved.
     */
    private final Map<UUID, UUID> settlementIdsByMember = new HashMap<>();

    public SettlementSavedData() {
        this.settlementsById = new LinkedHashMap<>();
    }

    private SettlementSavedData(@Nonnull SettlementSavedDataCodec.StoredState state) {
        this();
        for (Settlement decoded : state.settlements()) {
            Optional<Settlement> admissible = this.admissible(decoded);
            if (admissible.isEmpty()) {
                // Refused for its id when an earlier settlement holds it, and otherwise for its center chunk
                if (this.settlementsById.containsKey(decoded.id())) {
                    log.warn("Dropping a second stored settlement with id {}", decoded.id());
                } else {
                    log.warn("Dropping stored settlement {}: an earlier stored settlement claims its center chunk {}",
                            decoded.id(), decoded.bounds().center());
                }
                this.setDirty();
                continue;
            }

            if (!admissible.get().bounds().equals(decoded.bounds())) {
                log.warn("Clipping stored settlement {} to the chunks no earlier stored settlement claims",
                        decoded.id());
                this.setDirty();
            }

            this.put(this.withoutMembersListedEarlier(admissible.get()));
        }

        for (UUID tombstone : state.tombstones()) {
            // Only a hand-edited save stores both, and a living record is worth more than a ruin's tombstone
            if (this.settlementsById.containsKey(tombstone)) {
                log.warn("Dropping the tombstone of settlement {}: a stored settlement has its id", tombstone);
                this.setDirty();
                continue;
            }
            this.tombstones.add(tombstone);
        }
    }

    public static SettlementSavedData get(@Nonnull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SettlementSavedData::new, SettlementSavedData::load),
                DATA_NAME
        );
    }

    /**
     * Reads a store saved by {@link #save}.
     * Settlements that cannot be read are dropped. The rest are admitted in stored order by the same rule as {@link #add},
     * except that a roster row listing a member an earlier settlement lists is dropped, where add would throw, and
     * that a stored tombstone does not refuse a settlement: the tombstone is dropped instead.
     * An NBT tag that is not parsable loads empty with an error logged, and the level's next save overwrites it.
     */
    public static SettlementSavedData load(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        SettlementSavedDataCodec.StoredState state = SettlementSavedDataCodec.STATE_CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> log.error("Settlement store is unreadable and loads empty: {}", error))
                .orElse(new SettlementSavedDataCodec.StoredState(List.of(), List.of()));
        return new SettlementSavedData(state);
    }

    public Optional<Settlement> find(@Nonnull UUID settlementId) {
        return Optional.ofNullable(this.settlementsById.get(settlementId));
    }

    /**
     * Returns the settlement claiming the given chunk.
     */
    public Optional<Settlement> findClaiming(@Nonnull ChunkPosition chunk) {
        UUID settlementId = this.settlementIdsByChunk.get(pack(chunk));
        return settlementId == null ? Optional.empty() : this.find(settlementId);
    }

    /**
     * Returns the settlement whose roster lists the given member.
     */
    public Optional<Settlement> findListing(@Nonnull UUID memberId) {
        UUID settlementId = this.settlementIdsByMember.get(memberId);
        return settlementId == null ? Optional.empty() : this.find(settlementId);
    }

    /**
     * Adds a settlement with its claim clipped to the chunks no existing settlement claims, keeping only the part still
     * joined to its center chunk by edges.
     *
     * @return the settlement as stored, or empty when a settlement with its id is already stored, its id is
     * tombstoned, or a stored settlement claims its center chunk; the store is unchanged when empty
     * @throws IllegalArgumentException when the settlement would be stored but lists a member another stored
     *                                  settlement lists; the store is unchanged then
     */
    public Optional<Settlement> add(@Nonnull Settlement settlement) {
        if (this.tombstones.contains(settlement.id())) {
            return Optional.empty();
        }

        Optional<Settlement> admissible = this.admissible(settlement);
        if (admissible.isPresent()) {
            this.requireMembersListedNowhereElse(admissible.get());
            this.put(admissible.get());
            this.setDirty();
        }
        return admissible;
    }

    /**
     * Replaces the stored settlement with the given id by the result of the operation applied to it, keeping its place
     * in the store's order.
     * <p>
     * Nothing is written, and the store is not marked dirty, when the operation returns the very settlement it was
     * given. Any other result is written, including an equal copy.
     *
     * @param operation applied to the stored settlement once; it must not change the store
     * @return the settlement as now stored, or empty when no settlement with the given id is stored
     * @throws IllegalArgumentException when the result has another id or another claim, or lists a member another
     *                                  stored settlement lists; the store is unchanged then
     */
    public Optional<Settlement> update(@Nonnull UUID settlementId, @Nonnull UnaryOperator<Settlement> operation) {
        Settlement stored = this.settlementsById.get(settlementId);
        if (stored == null) {
            return Optional.empty();
        }

        Settlement updated = operation.apply(stored);
        Validate.notNull(updated, "Update of settlement %s must return a settlement", settlementId);
        // Identity rather than equality: equality would compare whole records on every write, while an equal copy
        // costs only a redundant save
        if (updated == stored) {
            return Optional.of(stored);
        }
        Validate.isTrue(updated.id().equals(settlementId), "Update of settlement %s must not change its id to %s",
                settlementId, updated.id());
        // Only an add clips a claim against the others, so a claim changed here could overlap another settlement's
        Validate.isTrue(updated.bounds().equals(stored.bounds()), "Update of settlement %s must not change its claim",
                settlementId);
        // An unchanged roster lists exactly the members already indexed for this settlement
        if (updated.roster() != stored.roster()) {
            this.requireMembersListedNowhereElse(updated);
            this.unindexMembers(stored);
            this.indexMembers(updated);
        }

        // Replacing the value of a key the map holds keeps the key's place in the iteration order
        this.settlementsById.put(settlementId, updated);
        this.setDirty();
        return Optional.of(updated);
    }

    /**
     * Deletes the stored settlement with the given id, tombstoning its id when it was generated.
     *
     * @return the settlement as it was stored, or empty when no settlement with the given id is stored; the store is
     * unchanged then
     */
    public Optional<Settlement> disband(@Nonnull UUID settlementId) {
        Settlement disbanded = this.settlementsById.remove(settlementId);
        if (disbanded == null) {
            return Optional.empty();
        }

        this.unindexClaim(disbanded);
        this.unindexMembers(disbanded);
        // A founded settlement exists nowhere else, while a generated one's structure start would record it again
        if (disbanded.origin() == SettlementOrigin.GENERATED) {
            this.tombstones.add(settlementId);
        }
        this.setDirty();
        return Optional.of(disbanded);
    }

    /**
     * Returns whether a generated settlement with the given id was disbanded from this level.
     */
    public boolean isTombstoned(@Nonnull UUID settlementId) {
        return this.tombstones.contains(settlementId);
    }

    /**
     * Returns the stored settlements in the order they were added, as they are now.
     */
    public List<Settlement> all() {
        return List.copyOf(this.settlementsById.values());
    }

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        SettlementSavedDataCodec.STATE_CODEC.encodeStart(NbtOps.INSTANCE,
                        new SettlementSavedDataCodec.StoredState(this.all(), List.copyOf(this.tombstones)))
                .resultOrPartial(error -> log.error("Could not encode the settlement store: {}", error))
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .ifPresent(tag::merge);
        return tag;
    }

    /**
     * Returns the settlement as the store would admit it, its claim clipped to the chunks no stored settlement claims,
     * or empty when a settlement with its id is stored or a stored settlement claims its center chunk.
     */
    private Optional<Settlement> admissible(@Nonnull Settlement settlement) {
        if (this.settlementsById.containsKey(settlement.id())) {
            return Optional.empty();
        }

        Optional<SettlementBounds> claim = settlement.bounds()
                .clip(chunk -> this.settlementIdsByChunk.containsKey(pack(chunk)));
        if (claim.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(claim.get() == settlement.bounds()
                ? settlement
                : settlement.toBuilder().bounds(claim.get()).build());
    }

    private Settlement withoutMembersListedEarlier(@Nonnull Settlement settlement) {
        SettlementRoster roster = settlement.roster();
        for (SettlementRosterEntry entry : settlement.roster().entries()) {
            UUID listing = this.settlementIdsByMember.get(entry.memberId());
            if (listing != null) {
                log.warn("Dropping member {} from the roster of stored settlement {}: earlier stored settlement {} lists it",
                        entry.memberId(), settlement.id(), listing);
                roster = roster.without(entry.memberId());
                this.setDirty();
            }
        }
        return roster == settlement.roster() ? settlement : settlement.toBuilder().roster(roster).build();
    }

    private void requireMembersListedNowhereElse(@Nonnull Settlement settlement) {
        for (SettlementRosterEntry entry : settlement.roster().entries()) {
            UUID listing = this.settlementIdsByMember.get(entry.memberId());
            Validate.isTrue(listing == null || listing.equals(settlement.id()),
                    "Settlement %s cannot list member %s, whom settlement %s already lists", settlement.id(),
                    entry.memberId(), listing);
        }
    }

    private void put(@Nonnull Settlement settlement) {
        this.settlementsById.put(settlement.id(), settlement);
        this.indexClaim(settlement);
        this.indexMembers(settlement);
    }

    private void indexClaim(@Nonnull Settlement settlement) {
        for (ChunkPosition chunk : settlement.bounds().chunks()) {
            this.settlementIdsByChunk.put(pack(chunk), settlement.id());
        }
    }

    private void unindexClaim(@Nonnull Settlement settlement) {
        for (ChunkPosition chunk : settlement.bounds().chunks()) {
            this.settlementIdsByChunk.remove(pack(chunk));
        }
    }

    private void indexMembers(@Nonnull Settlement settlement) {
        for (SettlementRosterEntry entry : settlement.roster().entries()) {
            this.settlementIdsByMember.put(entry.memberId(), settlement.id());
        }
    }

    private void unindexMembers(@Nonnull Settlement settlement) {
        for (SettlementRosterEntry entry : settlement.roster().entries()) {
            this.settlementIdsByMember.remove(entry.memberId());
        }
    }

    private static long pack(@Nonnull ChunkPosition chunk) {
        return ChunkPos.asLong(chunk.x(), chunk.z());
    }

}
