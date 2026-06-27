package dev.breezes.settlements.domain.ai.memory;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-agent typed store for decaying memories, keyed by {@link MemoryType.DecayingSpatialMemoryType}.
 * <p>
 * Currently supports only the spatial block-resource observation type
 * ({@code List<GlobalPos>} with per-entry TTL decay). Additional decaying types
 * can be added as the migration progresses.
 * <p>
 * Transient: not serialized. Rebuilding from the index costs one scan period after load,
 * which is acceptable for block-resource observations (§4 of the v2 design doc).
 * <p>
 * This class is NOT thread-safe.
 */
public final class SettlementsMemoryStore {

    private final Map<String, DecayingSpatialObservationStore> spatialStores = new HashMap<>();

    /**
     * Returns the live, TTL-filtered, UNORDERED sites for the given decaying spatial memory type.
     * Empty when the store is absent or has no non-expired entries.
     * <p>
     * The list is intentionally unordered — consumers that need ordering (nearest-first, etc.)
     * should use the scored overload on {@link DecayingSpatialObservationStore#liveSites} directly,
     * or re-sort the result themselves.
     */
    public Optional<List<GlobalPos>> getSpatialMemory(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                                                      @Nonnull ResourceKey<Level> dimension,
                                                      long nowTick) {
        DecayingSpatialObservationStore store = this.spatialStores.get(type.identifier());
        if (store == null) {
            return Optional.empty();
        }

        return store.getMemoryView(dimension, nowTick);
    }

    /**
     * Updates an observation report into the store for the given decaying spatial memory type.
     * Creates the store on first observation if it does not yet exist.
     */
    public void updateSpatialObservation(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                                         @Nonnull ObservationReport report,
                                         long nowTick) {
        DecayingSpatialObservationStore store = this.spatialStores.computeIfAbsent(
                type.identifier(),
                ignored -> new DecayingSpatialObservationStore(type.retentionTicks(), type.maxEntries()));
        store.update(report, nowTick);
    }

    /**
     * Clears the observation store for the given decaying spatial memory type.
     */
    public void clearSpatialMemory(@Nonnull MemoryType.DecayingSpatialMemoryType type) {
        DecayingSpatialObservationStore store = this.spatialStores.get(type.identifier());
        if (store != null) {
            store.clear();
        }
    }

    /**
     * Returns true if the store for the given type has at least one non-expired entry.
     */
    public boolean hasSpatialMemory(@Nonnull MemoryType.DecayingSpatialMemoryType type, long nowTick) {
        DecayingSpatialObservationStore store = this.spatialStores.get(type.identifier());
        return store != null && store.hasLiveSites(nowTick);
    }

}
