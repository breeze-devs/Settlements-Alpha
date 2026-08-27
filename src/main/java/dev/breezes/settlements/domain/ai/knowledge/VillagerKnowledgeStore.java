package dev.breezes.settlements.domain.ai.knowledge;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-villager episodic knowledge store.
 * <p>
 * Holds the {@link KnowledgeEntry} facts this villager perceived first-hand, keyed by origin
 * observation id so that one occurrence occupies one entry however many times it is perceived.
 * <p>
 * Capacity is bounded by the instance max-entry setting. When the store is full, the oldest
 * entry (by admission tick) is evicted to make room — knowledge decays naturally.
 */
public final class VillagerKnowledgeStore {

    /**
     * Default maximum number of entries retained at any time.
     * Insertion-order FIFO eviction when full prevents unbounded memory growth.
     */
    public static final int MAX_ENTRIES = 200;

    /**
     * LinkedHashMap retains insertion order for cheap oldest-first eviction.
     * Key = originObservationId; the map is the single source of truth.
     */
    private final Map<UUID, KnowledgeEntry> entriesByOriginId;
    private final int maxEntries;

    public VillagerKnowledgeStore() {
        this(MAX_ENTRIES);
    }

    public VillagerKnowledgeStore(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        // accessOrder = false → insertion order is preserved, never reordered on get/read.
        // The oldest entry (first-inserted) is always at the head of the iterator, giving us
        // O(1) FIFO eviction without any access-tracking overhead.
        this.entriesByOriginId = new LinkedHashMap<>(maxEntries, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, KnowledgeEntry> eldest) {
                return this.size() > VillagerKnowledgeStore.this.maxEntries;
            }
        };
    }

    /**
     * Attempts to add an entry, returning a typed result describing what happened.
     * <p>
     * An origin id already present is left untouched: the stored entry is the same fact, and the
     * first admission of it is the one whose admission tick reflects when this villager learned it.
     */
    public AdmitResult admit(KnowledgeEntry entry) {
        // TODO: this is the seam a reinstated corroboration model plugs into — a repeat admission
        //  currently drops the fact that something confirmed a fact already held, which is
        //  information the entry has nowhere to record.
        if (this.entriesByOriginId.containsKey(entry.getOriginObservationId())) {
            return AdmitResult.IGNORED_DUPLICATE;
        }

        this.entriesByOriginId.put(entry.getOriginObservationId(), entry);

        return AdmitResult.NEW_ENTRY;
    }

    /**
     * Returns the entry for the given origin observation id, if present
     */
    public Optional<KnowledgeEntry> findByOriginId(UUID originObservationId) {
        return Optional.ofNullable(this.entriesByOriginId.get(originObservationId));
    }

    /**
     * Returns a live, read-only view of all entries in insertion order.
     * <p>
     * Hot-path scans use this to avoid allocating a full snapshot every tick while
     * still preventing callers from structurally mutating the store outside the
     * admission and eviction rules owned by this class.
     */
    public Collection<KnowledgeEntry> entriesView() {
        return Collections.unmodifiableCollection(this.entriesByOriginId.values());
    }

    /**
     * Returns true if the store already contains an entry with the given origin id
     */
    public boolean knows(UUID originObservationId) {
        return this.entriesByOriginId.containsKey(originObservationId);
    }

    public int size() {
        return this.entriesByOriginId.size();
    }

    public boolean isEmpty() {
        return this.entriesByOriginId.isEmpty();
    }

    public int maxEntries() {
        return this.maxEntries;
    }

}
