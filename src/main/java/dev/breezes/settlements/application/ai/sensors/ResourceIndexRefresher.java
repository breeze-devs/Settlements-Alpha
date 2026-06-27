package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.WorldScanExecutor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@CustomLog
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ResourceIndexRefresher {

    /**
     * Minecraft section edge length (do not change).
     */
    private static final int SECTION_SIZE = 16;

    /**
     * Face-neighbor reach in section units. One section trivially covers any matcher neighbor read
     * (≤ 2 blocks away), so snapshotting the 6 axis-aligned face-neighbors is sufficient.
     */
    private static final int NEIGHBOR_REACH = 1;

    private final WorldResourceIndex index;
    private final Set<BlockResource> resources;
    private final BlockResourceSensorConfig config;

    @WorldScanExecutor
    private final ExecutorService worldScanExecutor;

    /**
     * Per-level async state: in-flight section keys + main-thread arrivals queue.
     * Lives here (not on {@link WorldResourceIndex}) because the async lifecycle is
     * owned by the refresher; the index remains a pure main-thread data structure.
     */
    private final Map<ServerLevel, LevelAsyncState> asyncStateByLevel = new IdentityHashMap<>();

    /**
     * Called every server level tick on the main thread.
     * <ol>
     *   <li>Drain completed worker arrivals — apply or discard each (newest-wins).</li>
     *   <li>Drain up to BUDGET dirty sections; for each, fast-path or submit to worker.</li>
     * </ol>
     */
    public void refresh(@Nonnull ServerLevel level) {
        LevelAsyncState asyncState = this.asyncStateFor(level);

        // Drain arrivals first so that any section re-enqueued by queryAll during this same
        // tick picks up a freshly applied scan rather than being submitted again unnecessarily.
        this.drainArrivals(level, asyncState);

        List<Long> sections = this.index.drainDirty(level, this.config.scanBudgetSectionsPerTick());
        if (sections.isEmpty()) {
            return;
        }

        long nowTick = level.getGameTime();
        for (long sectionKey : sections) {
            this.processDirtySection(level, asyncState, sectionKey, nowTick);
        }
    }

    /**
     * Called on chunk unload: clears in-flight markers and pending arrivals for the evicted
     * chunk's sections so a stale worker result never resurrects a section for an unloaded chunk.
     */
    public void evictChunk(@Nonnull ServerLevel level, int chunkX, int chunkZ) {
        LevelAsyncState asyncState = this.asyncStateByLevel.get(level);
        if (asyncState == null) {
            return;
        }
        int minSectionY = level.getMinSection();
        int maxSectionY = level.getMaxSection();
        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            long key = SectionPos.asLong(chunkX, sectionY, chunkZ);
            asyncState.inFlight.remove(key);
            asyncState.arrivals.removeIf(a -> a.sectionKey() == key);
        }
    }

    /**
     * Drops all async state for the level on level unload. The index itself is cleared by
     * {@link WorldResourceIndex#clear(ServerLevel)}.
     */
    public void clearLevel(@Nonnull ServerLevel level) {
        this.asyncStateByLevel.remove(level);
    }

    private void drainArrivals(@Nonnull ServerLevel level, @Nonnull LevelAsyncState asyncState) {
        ScanArrival arrival;
        while ((arrival = asyncState.arrivals.poll()) != null) {
            long key = arrival.sectionKey();

            // Always clear in-flight regardless of outcome — section will re-enqueue if still dirty.
            asyncState.inFlight.remove(key);

            // Null scan = worker failure → drop, do not apply; let the section stay stale for
            // re-enqueue by the next queryAll (the index is a self-healing cache).
            if (arrival.scan() == null) {
                continue;
            }

            // Discard if the chunk has been unloaded since the snapshot was taken.
            int chunkX = SectionPos.x(key);
            int chunkZ = SectionPos.z(key);
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                continue;
            }

            // Newer scan wins
            SectionResourceScan existing = this.index.getScan(level, key);
            if (existing != null && existing.scannedAtTick() >= arrival.scan().scannedAtTick()) {
                continue;
            }

            this.index.apply(level, key, arrival.scan());
        }
    }

    private void processDirtySection(@Nonnull ServerLevel level,
                                     @Nonnull LevelAsyncState asyncState,
                                     long sectionKey,
                                     long nowTick) {
        // Prevents duplicate submissions when queryAll re-enqueues an in-flight section.
        if (asyncState.inFlight.contains(sectionKey)) {
            return;
        }

        int chunkX = SectionPos.x(sectionKey);
        int chunkZ = SectionPos.z(sectionKey);

        // Guard: chunk unloaded between enqueue and drain → drop, do not re-enqueue.
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }

        int sectionY = SectionPos.y(sectionKey);
        int sectionIndex = chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY));
        if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
            return;
        }

        LevelChunkSection targetSection = chunk.getSection(sectionIndex);

        // Fast path 1: empty section — apply an authoritative empty scan immediately.
        // Recording freshness is critical so the section doesn't stay "stale" and re-enqueue endlessly.
        if (targetSection.hasOnlyAir()) {
            this.index.apply(level, sectionKey, new SectionResourceScan(nowTick, Map.of()));
            return;
        }

        // Fast path 2: palette prefilter — compute which resources could possibly appear.
        // maybeHas is conservative (may over-report for the global palette) so it only ever
        // skips work, never drops real hits.
        Set<BlockResource> survivors = computeSurvivors(this.resources, targetSection);
        if (survivors.isEmpty()) {
            this.index.apply(level, sectionKey, new SectionResourceScan(nowTick, Map.of()));
            return;
        }

        // Take the main-thread snapshot — safe because there is no concurrent writer on the
        // server main thread, and the returned copy is independent and never mutated.
        Long2ObjectMap<PalettedContainer<BlockState>> snapshot = takeSnapshot(level, chunk, sectionY);

        // Capture primitives to cross the thread boundary cleanly — no Level reference in the lambda.
        int originX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
        int originY = SectionPos.sectionToBlockCoord(sectionY);
        int originZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();

        asyncState.inFlight.add(sectionKey);
        CompletableFuture
                .supplyAsync(
                        () -> matchSnapshot(snapshot, survivors,
                                originX, originY, originZ,
                                minBuildHeight, maxBuildHeight, nowTick),
                        this.worldScanExecutor)
                .whenComplete((scan, err) -> {
                    if (err != null) {
                        // On failure: post a null-scan arrival so the main-thread drain clears
                        // the in-flight marker. The section stays stale and queryAll re-enqueues it.
                        log.warn("Async section scan failed for sectionKey={}: {}", sectionKey, err.toString());
                        asyncState.arrivals.offer(new ScanArrival(sectionKey, null));
                        return;
                    }
                    asyncState.arrivals.offer(new ScanArrival(sectionKey, scan));
                });
    }

    /**
     * Iterates the 16³ snapshot in a single pass, testing each block against every surviving
     * resource matcher. Runs on a worker thread; only touches the snapshot, matchers, and
     * local primitives — no live-world access.
     */
    private static SectionResourceScan matchSnapshot(@Nonnull Long2ObjectMap<PalettedContainer<BlockState>> snapshot,
                                                     @Nonnull Set<BlockResource> survivors,
                                                     int originX,
                                                     int originY,
                                                     int originZ,
                                                     int minBuildHeight,
                                                     int maxBuildHeight,
                                                     long nowTick) {
        SnapshotBlockStateView view = new SnapshotBlockStateView(snapshot);
        Map<BlockResource, LongArrayList> hitAccumulators = new HashMap<>(survivors.size());

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Loop order mirrors vanilla ChunkAccess.findBlocks: Y-outer, Z, X-inner.
        for (int y = originY; y < originY + SECTION_SIZE; y++) {
            if (y < minBuildHeight || y >= maxBuildHeight) {
                continue;
            }
            for (int z = originZ; z < originZ + SECTION_SIZE; z++) {
                for (int x = originX; x < originX + SECTION_SIZE; x++) {
                    cursor.set(x, y, z);

                    // Read the state from the target section's snapshot container.
                    int lx = x & 15;
                    int ly = y & 15;
                    int lz = z & 15;
                    long targetKey = SectionPos.asLong(
                            SectionPos.blockToSectionCoord(x),
                            SectionPos.blockToSectionCoord(y),
                            SectionPos.blockToSectionCoord(z));
                    PalettedContainer<BlockState> container = snapshot.get(targetKey);
                    BlockState state = container != null
                            ? container.get(lx, ly, lz)
                            : Blocks.AIR.defaultBlockState();

                    // One state read, many matchers — mirrors existing scanSection efficiency.
                    for (BlockResource resource : survivors) {
                        if (resource.matcher().matches(cursor, state, view)) {
                            hitAccumulators
                                    .computeIfAbsent(resource, ignored -> new LongArrayList())
                                    .add(cursor.asLong());
                        }
                    }
                }
            }
        }

        if (hitAccumulators.isEmpty()) {
            return new SectionResourceScan(nowTick, Map.of());
        }

        Map<BlockResource, long[]> hitsByResource = new HashMap<>(hitAccumulators.size());
        for (Map.Entry<BlockResource, LongArrayList> entry : hitAccumulators.entrySet()) {
            hitsByResource.put(entry.getKey(), entry.getValue().toLongArray());
        }

        return new SectionResourceScan(nowTick, Collections.unmodifiableMap(hitsByResource));
    }

    /**
     * Copies the target section's {@link PalettedContainer} plus its six axis-aligned
     * face-neighbor section containers into a map keyed by {@code SectionPos.asLong()}.
     * <p>
     * One face-neighbor section trivially covers any matcher neighbor read (≤ 2 blocks
     * in any axis-aligned direction), so the worker never needs to touch the live world.
     * Unloaded neighbor chunks have no entry; the view falls back to air for those positions.
     */
    @Nonnull
    private static Long2ObjectMap<PalettedContainer<BlockState>> takeSnapshot(@Nonnull ServerLevel level,
                                                                              @Nonnull LevelChunk targetChunk,
                                                                              int targetSectionY) {
        Long2ObjectMap<PalettedContainer<BlockState>> snapshot = new Long2ObjectOpenHashMap<>();

        int chunkX = targetChunk.getPos().x;
        int chunkZ = targetChunk.getPos().z;

        // Snapshot target + horizontal face-neighbor chunks (±X, ±Z), all at targetSectionY.
        for (int dx = -NEIGHBOR_REACH; dx <= NEIGHBOR_REACH; dx++) {
            for (int dz = -NEIGHBOR_REACH; dz <= NEIGHBOR_REACH; dz++) {
                // Only face-aligned neighbors, not diagonals.
                if (dx != 0 && dz != 0) {
                    continue;
                }
                LevelChunk neighborChunk = (dx == 0 && dz == 0)
                        ? targetChunk
                        : level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (neighborChunk == null) {
                    continue;
                }
                snapshotSectionY(neighborChunk, chunkX + dx, targetSectionY, snapshot, level);
            }
        }

        // Snapshot the vertical face-neighbors (±Y) within the target chunk.
        snapshotSectionY(targetChunk, chunkX, targetSectionY - NEIGHBOR_REACH, snapshot, level);
        snapshotSectionY(targetChunk, chunkX, targetSectionY + NEIGHBOR_REACH, snapshot, level);

        return snapshot;
    }

    /**
     * Copies one section from the given chunk into the snapshot map, skipping out-of-range
     * section indices. The key uses the passed {@code chunkX} and the chunk's actual Z so
     * horizontal neighbors are keyed correctly.
     */
    private static void snapshotSectionY(@Nonnull LevelChunk chunk,
                                         int chunkX,
                                         int sectionY,
                                         @Nonnull Long2ObjectMap<PalettedContainer<BlockState>> snapshot,
                                         @Nonnull ServerLevel level) {
        if (sectionY < level.getMinSection() || sectionY > level.getMaxSection()) {
            return;
        }
        int sectionIndex = chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY));
        if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
            return;
        }
        LevelChunkSection section = chunk.getSection(sectionIndex);
        long key = SectionPos.asLong(chunkX, sectionY, chunk.getPos().z);
        // copy() is safe on the main thread — no concurrent writer here.
        snapshot.put(key, section.getStates().copy());
    }

    /**
     * Returns the subset of resources whose state predicate passes {@code maybeHas}
     * on the live target section (main thread). Conservative — only skips, never
     * drops real hits.
     */
    @Nonnull
    private static Set<BlockResource> computeSurvivors(@Nonnull Set<BlockResource> resources,
                                                       @Nonnull LevelChunkSection section) {
        Set<BlockResource> survivors = new HashSet<>(resources.size());
        for (BlockResource resource : resources) {
            if (section.maybeHas(resource.matcher().statePredicate())) {
                survivors.add(resource);
            }
        }
        return survivors;
    }

    private LevelAsyncState asyncStateFor(@Nonnull ServerLevel level) {
        return this.asyncStateByLevel.computeIfAbsent(level, ignored -> new LevelAsyncState());
    }

    /**
     * Per-level async bookkeeping. Both fields have distinct access patterns:
     * <ul>
     *   <li>{@code inFlight} is written only on the main thread (add on submit, remove on drain);
     *       the worker posts a null-arrival instead of removing directly.</li>
     *   <li>{@code arrivals} is written by worker threads and drained on the main thread — the
     *       {@link ConcurrentLinkedQueue} provides the necessary thread safety.</li>
     * </ul>
     */
    private static final class LevelAsyncState {

        private final LongOpenHashSet inFlight = new LongOpenHashSet();
        private final ConcurrentLinkedQueue<ScanArrival> arrivals = new ConcurrentLinkedQueue<>();

    }

    /**
     * Carries a completed (or failed) scan back to the main thread.
     * A {@code null} scan signals worker failure; the drain loop clears in-flight and discards.
     */
    private record ScanArrival(long sectionKey, @Nullable SectionResourceScan scan) {
    }

}
