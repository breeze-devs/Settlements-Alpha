package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.di.ServerScope;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is NOT thread safe. It must be run on the main thread.
 */
@ServerScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class WorldResourceIndex {

    /**
     * One LevelResourceIndex per loaded dimension — per-level isolation prevents
     * cross-dimensional memory leaks and simplifies level-unload cleanup.
     */
    private final Map<ServerLevel, LevelResourceIndex> indexesByLevel = new IdentityHashMap<>();

    /**
     * Enumerates the intersecting sections ONCE, reads each scan ONCE, and fans
     * hits into per-resource result buckets in a single pass — replacing N independent
     * query() calls that each re-enumerated the same section set.
     */
    public Map<BlockResource, BlockResourceQueryResult> queryAll(@Nonnull ServerLevel level,
                                                                 @Nonnull Set<BlockResource> resources,
                                                                 @Nonnull BlockPos anchor,
                                                                 int horizontalRadius,
                                                                 int verticalRadius,
                                                                 long nowTick,
                                                                 long ttlTicks) {
        LevelResourceIndex levelIndex = this.indexFor(level);

        // Pre-allocate one accumulator list per resource to avoid repeated map lookups inside the hot loop.
        Map<BlockResource, List<long[]>> accumByResource = new IdentityHashMap<>(resources.size());
        for (BlockResource resource : resources) {
            accumByResource.put(resource, new ArrayList<>());
        }

        // A single completeness flag shared across all resources: section freshness is a property
        // of the section, not the resource, so all resources get the same complete value.
        boolean complete = true;

        // Enumerate intersecting sections exactly once — the key cost reduction for N-resource sensors.
        long[] sectionKeys = sectionsIntersecting(anchor, horizontalRadius, verticalRadius);
        for (long sectionKey : sectionKeys) {
            SectionResourceScan scan = levelIndex.sections.get(sectionKey);
            boolean sectionFresh = scan != null && (nowTick - scan.scannedAtTick()) < ttlTicks;

            if (!sectionFresh) {
                levelIndex.enqueue(sectionKey);
                // Any stale or missing section makes the entire query incomplete for all resources.
                complete = false;
            }

            if (scan != null) {
                // Fan this section's hits into each resource's accumulator.
                for (BlockResource resource : resources) {
                    long[] hits = scan.hitsFor(resource);
                    if (hits.length > 0) {
                        accumByResource.get(resource).add(hits);
                    }
                }
            }
        }

        // Build per-resource results: box-filter and apply the nearest-K cap per resource
        Map<BlockResource, BlockResourceQueryResult> results = new IdentityHashMap<>(resources.size());
        for (BlockResource resource : resources) {
            List<long[]> chunks = accumByResource.get(resource);
            results.put(resource, buildResult(chunks, complete, anchor, horizontalRadius, verticalRadius, resource.memoryType().maxEntries()));
        }
        return results;
    }

    public List<Long> drainDirty(@Nonnull ServerLevel level, int maxSections) {
        LevelResourceIndex levelIndex = this.indexFor(level);
        List<Long> drained = new ArrayList<>(maxSections);

        while (drained.size() < maxSections && !levelIndex.dirtyQueue.isEmpty()) {
            long sectionKey = levelIndex.dirtyQueue.removeFirstLong();
            drained.add(sectionKey);
        }

        return drained;
    }

    public void apply(@Nonnull ServerLevel level, long sectionKey, @Nonnull SectionResourceScan scan) {
        this.indexFor(level).sections.put(sectionKey, scan);
    }

    /**
     * Returns the current scan for the given section, or {@code null} if none exists.
     * Used by the refresher for newest-wins comparison when draining async arrivals.
     */
    @Nullable
    public SectionResourceScan getScan(@Nonnull ServerLevel level, long sectionKey) {
        LevelResourceIndex levelIndex = this.indexesByLevel.get(level);
        if (levelIndex == null) {
            return null;
        }
        return levelIndex.sections.get(sectionKey);
    }

    /**
     * Removes all sections belonging to the given chunk (all section-Y for this chunk X/Z),
     * both from the index and from the dirty queue, on chunk unload.
     * <p>
     * {@code LongLinkedOpenHashSet} supports O(1) {@code remove}, so we clean both the data
     * map and the dirty queue in one pass without any deferred "skip on drain" logic.
     */
    public void evictChunk(@Nonnull ServerLevel level, int chunkX, int chunkZ) {
        LevelResourceIndex levelIndex = this.indexFor(level);
        int minSectionY = level.getMinSection();
        int maxSectionY = level.getMaxSection();
        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            long key = SectionPos.asLong(chunkX, sectionY, chunkZ);
            levelIndex.sections.remove(key);
            levelIndex.dirtyQueue.remove(key);
        }
    }

    public void clear(@Nonnull ServerLevel level) {
        this.indexesByLevel.remove(level);
    }

    private LevelResourceIndex indexFor(@Nonnull ServerLevel level) {
        return this.indexesByLevel.computeIfAbsent(level, ignored -> new LevelResourceIndex());
    }

    /**
     * Computes the packed SectionPos keys for all sections intersecting the query box.
     */
    private static long[] sectionsIntersecting(@Nonnull BlockPos anchor,
                                               int horizontalRadius,
                                               int verticalRadius) {
        int minSectionX = SectionPos.blockToSectionCoord(anchor.getX() - horizontalRadius);
        int maxSectionX = SectionPos.blockToSectionCoord(anchor.getX() + horizontalRadius);
        int minSectionY = SectionPos.blockToSectionCoord(anchor.getY() - verticalRadius);
        int maxSectionY = SectionPos.blockToSectionCoord(anchor.getY() + verticalRadius);
        int minSectionZ = SectionPos.blockToSectionCoord(anchor.getZ() - horizontalRadius);
        int maxSectionZ = SectionPos.blockToSectionCoord(anchor.getZ() + horizontalRadius);

        int count = (maxSectionX - minSectionX + 1) * (maxSectionY - minSectionY + 1) * (maxSectionZ - minSectionZ + 1);
        long[] keys = new long[count];
        int idx = 0;

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    keys[idx++] = SectionPos.asLong(sectionX, sectionY, sectionZ);
                }
            }
        }

        return keys;
    }

    /**
     * Box-filters section hits and retains at most K nearest to the anchor in a single heap pass
     */
    private static BlockResourceQueryResult buildResult(@Nonnull List<long[]> chunks,
                                                        boolean complete,
                                                        @Nonnull BlockPos anchor,
                                                        int horizontalRadius,
                                                        int verticalRadius,
                                                        int k) {
        NearestKSelector.SelectionResult selection = NearestKSelector.select(
                chunks, anchor.getX(), anchor.getY(), anchor.getZ(), horizontalRadius, verticalRadius, k);

        return new BlockResourceQueryResult(selection.sites(), complete, selection.truncated());
    }

    private static final class LevelResourceIndex {

        // Packed SectionPos long → scan result; faster than HashMap<SectionPos,...> because
        // SectionPos is a record that boxes, and this avoids all boxing on the hot query path.
        private final Long2ObjectOpenHashMap<SectionResourceScan> sections = new Long2ObjectOpenHashMap<>();

        // FIFO queue for dirty sections; LongLinkedOpenHashSet gives dedup + FIFO + O(1) remove
        // in one structure — no separate membership set needed.
        private final LongLinkedOpenHashSet dirtyQueue = new LongLinkedOpenHashSet();

        private void enqueue(long sectionKey) {
            // LongLinkedOpenHashSet.add() is a no-op when the key is already present,
            // so this provides dedup for free.
            this.dirtyQueue.add(sectionKey);
        }

    }

}
