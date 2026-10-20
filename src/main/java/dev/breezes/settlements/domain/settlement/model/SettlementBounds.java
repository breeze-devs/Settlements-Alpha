package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.world.ChunkPosition;
import org.apache.commons.lang3.Validate;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The chunks a settlement claims in its dimension. The claim is one region of chunks joined by their edges, and its
 * center is one of them.
 *
 * @param center the chunk the settlement is centered on
 * @param chunks the claimed chunks
 */
public record SettlementBounds(ChunkPosition center, Set<ChunkPosition> chunks) {

    private static final int[][] EDGE_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public SettlementBounds {
        Validate.notNull(center, "Settlement bounds center must not be null");
        Validate.notNull(chunks, "Settlement bounds chunks must not be null around %s", center);
        chunks = Set.copyOf(chunks);
        Validate.isTrue(chunks.contains(center), "Settlement bounds must claim their center chunk %s", center);
        // One region exactly when the flood from the center reaches every claimed chunk
        Validate.isTrue(edgeRegion(center, chunks::contains).size() == chunks.size(),
                "Settlement bounds must be connected around %s", center);
    }

    /**
     * Returns whether the settlement claims the given chunk.
     */
    public boolean contains(ChunkPosition chunk) {
        return this.chunks.contains(chunk);
    }

    /**
     * Returns the claimed chunks in rings outward from the center chunk.
     * The order within a ring is unspecified.
     */
    public List<ChunkPosition> centerFirst() {
        return this.chunks.stream()
                .sorted(Comparator.comparingInt(this::ringOf))
                .toList();
    }

    /**
     * Returns this claim without the chunks another settlement already claims, keeping only the part still joined to
     * the center chunk by edges. Returns this same instance when none of its chunks is claimed elsewhere, and empty when
     * the center chunk itself is.
     *
     * @param claimedElsewhere accepts each chunk another settlement already claims
     */
    public Optional<SettlementBounds> clip(Predicate<ChunkPosition> claimedElsewhere) {
        if (claimedElsewhere.test(this.center)) {
            return Optional.empty();
        }
        // Claims almost never overlap, so one pass of lookups spares the common case the flood and a new claim
        if (this.chunks.stream().noneMatch(claimedElsewhere)) {
            return Optional.of(this);
        }
        return Optional.of(new SettlementBounds(this.center,
                edgeRegion(this.center, chunk -> this.chunks.contains(chunk) && !claimedElsewhere.test(chunk))));
    }

    private int ringOf(ChunkPosition chunk) {
        return Math.max(Math.abs(chunk.x() - this.center.x()), Math.abs(chunk.z() - this.center.z()));
    }

    private static Set<ChunkPosition> edgeRegion(ChunkPosition center, Predicate<ChunkPosition> inRegion) {
        // Flood across shared edges from the center through the chunks inRegion admits; the center seeds the flood
        // without being tested. Corners do not join chunks, so two regions touching only diagonally stay apart.
        Set<ChunkPosition> reached = new HashSet<>();
        Deque<ChunkPosition> frontier = new ArrayDeque<>();
        reached.add(center);
        frontier.add(center);
        while (!frontier.isEmpty()) {
            ChunkPosition chunk = frontier.poll();
            for (int[] offset : EDGE_OFFSETS) {
                ChunkPosition neighbor = new ChunkPosition(chunk.x() + offset[0], chunk.z() + offset[1]);
                if (inRegion.test(neighbor) && reached.add(neighbor)) {
                    frontier.add(neighbor);
                }
            }
        }
        return reached;
    }

}
