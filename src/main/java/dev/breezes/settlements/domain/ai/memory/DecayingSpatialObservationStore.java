package dev.breezes.settlements.domain.ai.memory;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Per-agent decaying spatial observation store for a single block resource type.
 * <p>
 * Stores timestamped site observations as packed {@code BlockPos.asLong()} keys in a fastutil map.
 * Decay is lazy: entries past their TTL are filtered on read and purged opportunistically during fold operations.
 * A size cap with stalest-first eviction bounds memory for roaming villagers.
 * <p>
 * This class is NOT thread-safe.
 */
public final class DecayingSpatialObservationStore {

    private static final int DEFAULT_MAX_ENTRIES = 256;

    private final long retentionTicks;
    private final int maxEntries;
    private final Long2ObjectOpenHashMap<SiteObservation> observations;

    public DecayingSpatialObservationStore(long retentionTicks, int maxEntries) {
        this.retentionTicks = retentionTicks;
        this.maxEntries = maxEntries;
        this.observations = new Long2ObjectOpenHashMap<>(Math.min(maxEntries, 64));
    }

    public DecayingSpatialObservationStore(long retentionTicks) {
        this(retentionTicks, DEFAULT_MAX_ENTRIES);
    }

    /**
     * Updates this store with an observation report, then evicts stalest entries if the size cap was breached.
     * Expiry filtering happens on the entries already present before the update.
     */
    public void update(@Nonnull ObservationReport report, long nowTick) {
        // Eagerly expire before folding
        this.expireStale(nowTick);
        report.update(this.observations);

        // Cap enforcement: remove the stalest entries if we exceeded maxEntries
        this.evictStalestIfNeeded();
    }

    /**
     * Returns a live, TTL-filtered, UNORDERED list of {@code GlobalPos} for all remembered sites.
     * Expired entries are filtered out and removed opportunistically.
     * <p>
     * Callers that need ordering (e.g. distance-nearest-first) should use
     * {@link #liveSites(ResourceKey, int, int, int, long, SiteScorer)} with the desired scorer.
     * The two known consumers — {@code BlockMemoryTargetResolver.confirmNearest} (re-sorts by true
     * distance itself) and precondition checks (existence only) — do not require a pre-ordered list,
     * so the sort here would be wasted work.
     *
     * @param dimension the dimension key to embed in each {@code GlobalPos}
     * @param nowTick   current game tick used to evaluate per-entry TTL
     */
    public List<GlobalPos> liveView(@Nonnull ResourceKey<Level> dimension, long nowTick) {
        long cutoff = nowTick - this.retentionTicks;
        List<GlobalPos> result = new ArrayList<>(this.observations.size());
        List<Long> toRemove = null;

        for (Long2ObjectMap.Entry<SiteObservation> entry : this.observations.long2ObjectEntrySet()) {
            if (entry.getValue().lastSeenTick() < cutoff) {
                // Lazy expiry: collect for removal, do not remove inside the entry-set iteration.
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(entry.getLongKey());
                continue;
            }

            result.add(GlobalPos.of(dimension, BlockPos.of(entry.getLongKey())));
        }

        if (toRemove != null) {
            for (long key : toRemove) {
                this.observations.remove(key);
            }
        }

        return result;
    }

    /**
     * Returns a live, TTL-filtered list of {@code GlobalPos} sorted by the given {@link SiteScorer}.
     * Lower score = higher priority (e.g. nearer for the distance scorer).
     * <p>
     * Expired entries are filtered out and removed opportunistically.
     * This overload is intended for the P6 context-aware planner and any other consumer
     * that needs an ordered view; the default read path should use the zero-argument
     * {@link #liveView(ResourceKey, long)} overload to avoid the sort overhead.
     *
     * @param dimension the dimension key to embed in each {@code GlobalPos}
     * @param originX   X coordinate of the querying entity
     * @param originY   Y coordinate of the querying entity
     * @param originZ   Z coordinate of the querying entity
     * @param nowTick   current game tick used to evaluate per-entry TTL
     * @param scorer    ordering function; lower score = higher priority
     */
    public List<GlobalPos> liveSites(@Nonnull ResourceKey<Level> dimension,
                                     int originX, int originY, int originZ,
                                     long nowTick,
                                     @Nonnull SiteScorer scorer) {
        long cutoff = nowTick - this.retentionTicks;
        List<ScoredSite> liveEntries = new ArrayList<>(this.observations.size());
        List<Long> toRemove = null;

        for (Long2ObjectMap.Entry<SiteObservation> entry : this.observations.long2ObjectEntrySet()) {
            if (entry.getValue().lastSeenTick() < cutoff) {
                // Lazy expiry: collect for removal, do not remove inside the entry-set iteration.
                if (toRemove == null) {
                    toRemove = new ArrayList<>();
                }
                toRemove.add(entry.getLongKey());
                continue;
            }

            double score = scorer.score(entry.getLongKey(), originX, originY, originZ,
                    entry.getValue().lastSeenTick(), nowTick);
            liveEntries.add(new ScoredSite(entry.getLongKey(), score));
        }

        if (toRemove != null) {
            for (long key : toRemove) {
                this.observations.remove(key);
            }
        }

        if (liveEntries.isEmpty()) {
            return List.of();
        }

        liveEntries.sort(Comparator.comparingDouble(ScoredSite::score));

        List<GlobalPos> result = new ArrayList<>(liveEntries.size());
        for (ScoredSite scored : liveEntries) {
            result.add(GlobalPos.of(dimension, BlockPos.of(scored.packed())));
        }

        return result;
    }

    /**
     * Returns true if this store has at least one non-expired entry.
     * <p>
     * Expired entries are not pruned by this check — use {@link #liveView} for a clean view.
     */
    public boolean hasLiveSites(long nowTick) {
        long cutoff = nowTick - this.retentionTicks;
        for (var entry : this.observations.long2ObjectEntrySet()) {
            if (entry.getValue().lastSeenTick() >= cutoff) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the live-sites list — empty when there are no non-expired entries, present (possibly non-empty) otherwise.
     * The returned list is live, TTL-filtered, and UNORDERED.
     * <p>
     * This is the view that {@code IBrain.getMemory} returns to callers expecting {@code Optional<List<GlobalPos>>}.
     */
    public Optional<List<GlobalPos>> getMemoryView(@Nonnull ResourceKey<Level> dimension, long nowTick) {
        List<GlobalPos> sites = this.liveView(dimension, nowTick);
        if (sites.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(sites);
    }

    /**
     * Clears all entries
     */
    public void clear() {
        this.observations.clear();
    }

    /**
     * Returns the number of entries currently in the store, including potentially-expired ones.
     */
    public int size() {
        return this.observations.size();
    }

    private void expireStale(long nowTick) {
        long cutoff = nowTick - this.retentionTicks;
        this.observations.long2ObjectEntrySet().removeIf(e -> e.getValue().lastSeenTick() < cutoff);
    }

    /**
     * If the store exceeds {@code maxEntries}, removes the entries with the smallest
     * {@code lastSeenTick} (i.e. oldest / stalest) until within bounds.
     */
    private void evictStalestIfNeeded() {
        if (this.observations.size() <= this.maxEntries) {
            return;
        }

        // Collect all entries, sort by tick ascending, remove the excess oldest ones.
        int excess = this.observations.size() - this.maxEntries;
        this.observations.long2ObjectEntrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().lastSeenTick()))
                .limit(excess)
                .map(Long2ObjectMap.Entry::getLongKey)
                .forEach(key -> this.observations.remove(key.longValue()));
    }

    /**
     * Carrier for a packed position and its scorer-computed priority; used only inside
     * the scored overload of {@link #liveSites} to avoid the {@code long[2]}/bit-cast trick.
     */
    private record ScoredSite(long packed, double score) {
    }

}
