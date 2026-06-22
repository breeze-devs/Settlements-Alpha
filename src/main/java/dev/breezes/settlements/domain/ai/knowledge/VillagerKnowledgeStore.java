package dev.breezes.settlements.domain.ai.knowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-villager episodic knowledge store.
 * <p>
 * This is the store the perception pipeline will promote into. It holds {@link KnowledgeEntry} records that were either:
 * <ul>
 *   <li>Directly observed by this villager and promoted by {@link dev.breezes.settlements.application.ai.memory.MemoryImportanceGate}.</li>
 *   <li>Received hearsay from another villager during a gossip exchange.</li>
 * </ul>
 * <p>
 * Capacity is bounded by the instance max-entry setting. When the store is full, the oldest
 * entry (by admission tick) is evicted to make room — knowledge decays naturally.
 * This class carries no Minecraft state and is fully unit-testable without mocks.
 */
public final class VillagerKnowledgeStore {

    /**
     * Default maximum number of entries retained at any time.
     * Insertion-order FIFO eviction when full prevents unbounded memory growth.
     */
    public static final int MAX_ENTRIES = 200;

    /**
     * Absolute weight bump applied to an existing entry when a second independent
     * source corroborates the same fact. Intentionally modest — corroboration
     * signals additional confidence, not a proportional increase in magnitude.
     */
    public static final float CORROBORATION_BUMP = 0.1f;

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
     * Also applies corroboration: if the origin-id is already known from a different independent
     * source, the existing entry's corroboration count and weight are bumped and
     * {@link AdmitResult#CORROBORATED_EXISTING} is returned so callers can act on the distinction.
     */
    public AdmitResult admit(KnowledgeEntry entry) {
        // Limit hop cap — only entries within the propagation limit are stored.
        if (entry.getHop() > KnowledgeEntry.MAX_HOP_COUNT) {
            return AdmitResult.REJECTED_HOP_CAP;
        }

        // Corroboration-aware dedupe
        // - a different independent source confirming the same fact bumps its confidence;
        // - the same source re-sharing is a pure no-op (no new information).
        KnowledgeEntry existing = this.entriesByOriginId.get(entry.getOriginObservationId());
        if (existing != null) {
            if (!Objects.equals(existing.getSource(), entry.getSource())) {
                // Independent corroboration: different witness, same fact.
                existing.corroborate(CORROBORATION_BUMP);
                return AdmitResult.CORROBORATED_EXISTING;
            }
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
     * Returns entries that are candidates for gossip sharing
     */
    public List<KnowledgeEntry> shareableEntries() {
        Collection<KnowledgeEntry> values = this.entriesByOriginId.values();
        List<KnowledgeEntry> shareable = new ArrayList<>(values.size());

        for (KnowledgeEntry entry : values) {
            if (entry.isShareable()) {
                shareable.add(entry);
            }
        }
        return shareable;
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
