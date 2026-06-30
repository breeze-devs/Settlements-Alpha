package dev.breezes.settlements.domain.ai.memory;

import net.minecraft.core.GlobalPos;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable projection of a villager's spatial site memories at a single point in time.
 * <p>
 * Covers every memory whose payload is a {@code List<GlobalPos>} — the decaying block-resource
 * sites and the vanilla-backed cultivation totem sites alike — keyed off their shared
 * {@link MemoryType} supertype so both kinds ride one snapshot.
 * <p>
 * Only types that had at least one live site at read-time appear as keys in the internal map;
 * absent types return a safe sentinel value from the derived accessors rather than throwing,
 * so consumers can query any {@code MemoryType<List<GlobalPos>>} without first checking
 * for key presence.
 * <p>
 * Exposes two read shapes that the roadmap's Phase 3 (planner) and Phase 4 (snapshot) need:
 * <ul>
 *   <li>Coord-dump via {@link #coordsByType()} / {@link #coords(MemoryType)}</li>
 *   <li>Presence/count via {@link #isPresent(MemoryType)} / {@link #count(MemoryType)}</li>
 * </ul>
 * Built once from {@link SensedSiteReader#read(dev.breezes.settlements.domain.ai.brain.IBrain)};
 * both consumers ride this single object.
 */
public final class SensedSites {

    private final Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> coordsByType;

    /**
     * @param sites the coord lists keyed by memory type; defensively copied on construction
     *              so the caller may mutate the input map and lists without affecting this instance
     */
    public SensedSites(@Nonnull Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> sites) {
        Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> copy = new HashMap<>();
        for (Map.Entry<MemoryType<List<GlobalPos>>, List<SiteCoord>> entry : sites.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.coordsByType = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the unmodifiable map of all sensed site types to their coord lists.
     * Only types with at least one live site appear as keys.
     */
    public Map<MemoryType<List<GlobalPos>>, List<SiteCoord>> coordsByType() {
        return this.coordsByType;
    }

    /**
     * Returns the coords for the given type, or an empty list if none were sensed.
     */
    public List<SiteCoord> coords(@Nonnull MemoryType<List<GlobalPos>> type) {
        return this.coordsByType.getOrDefault(type, List.of());
    }

    /**
     * Returns how many sites of the given type were sensed, or 0 if none.
     */
    public int count(@Nonnull MemoryType<List<GlobalPos>> type) {
        return this.coords(type).size();
    }

    /**
     * Returns whether any sites of the given type were sensed.
     */
    public boolean isPresent(@Nonnull MemoryType<List<GlobalPos>> type) {
        return this.count(type) > 0;
    }

}
