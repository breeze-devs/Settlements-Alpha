package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.PackedPos;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Helper that selects up to K nearest packed block positions within a
 * box, using a bounded max-heap to avoid materializing all in-box hits before sorting.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class NearestKSelector {

    /**
     * Result of a bounded nearest-K selection.
     *
     * @param sites     up to K packed {@code BlockPos.asLong()} longs nearest the anchor
     * @param truncated {@code true} when the number of in-box hits exceeded K; a truncated
     *                  result is not authoritative for absence — it only knows the K nearest
     */
    public record SelectionResult(@Nonnull LongList sites, boolean truncated) {
    }

    /**
     * Filters section hit arrays to the query box (an axis-aligned Chebyshev / L-infinity test)
     * and selects the up to K nearest to the anchor by squared Euclidean distance on x/y/z.
     * <p>
     * A size-K max-heap is maintained during the pass: the heap root is always the farthest
     * of the current K candidates. When a new in-box hit is closer than the root, we evict
     * the root and insert the closer hit. This keeps worst-case heap size at K+1 and avoids
     * sorting the full hit set.
     *
     * @param chunks           per-section arrays of packed position longs from the index
     * @param anchorX          anchor block X coordinate
     * @param anchorY          anchor block Y coordinate
     * @param anchorZ          anchor block Z coordinate
     * @param horizontalRadius box half-width on X and Z axes
     * @param verticalRadius   box half-height on Y axis
     * @param k                maximum number of sites to return
     */
    public static SelectionResult select(@Nonnull List<long[]> chunks,
                                         int anchorX,
                                         int anchorY,
                                         int anchorZ,
                                         int horizontalRadius,
                                         int verticalRadius,
                                         int k) {
        // Max-heap by squared distance — root is always the farthest of the current K candidates
        // Evicting the farthest when a closer hit arrives lets us bound heap size at K
        PriorityQueue<long[]> heap = new PriorityQueue<>(k + 1,
                (a, b) -> Long.compare(b[1], a[1])); // descending by distSq (index 1)

        int inBoxCount = 0;

        for (long[] chunk : chunks) {
            for (long packed : chunk) {
                int x = PackedPos.x(packed);
                int y = PackedPos.y(packed);
                int z = PackedPos.z(packed);

                if (!inBox(x, y, z, anchorX, anchorY, anchorZ, horizontalRadius, verticalRadius)) {
                    continue;
                }

                inBoxCount++;

                long distSq = squaredDist(x, y, z, anchorX, anchorY, anchorZ);

                if (heap.size() < k) {
                    // Heap not full yet — always add
                    heap.offer(new long[]{packed, distSq});
                } else if (distSq < heap.peek()[1]) {
                    // This hit is closer than the current farthest candidate — evict and insert
                    heap.poll();
                    heap.offer(new long[]{packed, distSq});
                }
                // Otherwise this hit is farther than all K candidates — skip it
            }
        }

        // Drain the heap into a LongArrayList. Ordering is arbitrary (heap order), which is fine —
        // consumers sort by their own criteria (memory store uses TTL, behaviors use proximity at use-time).
        LongArrayList result = new LongArrayList(heap.size());
        for (long[] entry : heap) {
            result.add(entry[0]);
        }

        // Truncated when more in-box hits existed than the cap could accommodate.
        boolean truncated = inBoxCount > k;

        return new SelectionResult(result, truncated);
    }

    private static boolean inBox(int x, int y, int z,
                                 int anchorX, int anchorY, int anchorZ,
                                 int horizontalRadius, int verticalRadius) {
        return Math.abs(x - anchorX) <= horizontalRadius
                && Math.abs(z - anchorZ) <= horizontalRadius
                && Math.abs(y - anchorY) <= verticalRadius;
    }

    private static long squaredDist(int x, int y, int z, int anchorX, int anchorY, int anchorZ) {
        long dx = x - anchorX;
        long dy = y - anchorY;
        long dz = z - anchorZ;
        return dx * dx + dy * dy + dz * dz;
    }

}
