package dev.breezes.settlements.domain.settlement.founding;

import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.world.ChunkPosition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The claim a settlement takes when it is founded at a bell.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementFoundingClaim {

    /**
     * The smallest radius a founding claims, in chunks from the bell's chunk.
     */
    public static final int MIN_RADIUS = 2;

    /**
     * The largest radius a founding claims, in chunks from the bell's chunk.
     * Places farther away stay outside the claim.
     */
    public static final int MAX_RADIUS = 5;

    /**
     * How many chunks a claim reaches past the farthest place it encloses, so a house at the edge keeps a margin.
     */
    public static final int MARGIN = 1;

    /**
     * How many chunks the smallest claim a founding takes holds.
     */
    public static final int MIN_SQUARE_CHUNKS = (2 * MIN_RADIUS + 1) * (2 * MIN_RADIUS + 1);

    /**
     * Returns the square a settlement founded at the given chunk claims before other settlements' claims are skipped.
     * With no places to enclose, it is the smallest square.
     *
     * @param center   the bell's chunk, which becomes the claim's center
     * @param enclosed the chunks the claim should enclose: those of the founders' beds and workstations
     */
    public static SettlementBounds square(ChunkPosition center, Collection<ChunkPosition> enclosed) {
        int farthest = enclosed.stream()
                .mapToInt(chunk -> Math.max(Math.abs(chunk.x() - center.x()), Math.abs(chunk.z() - center.z())))
                .max()
                .orElse(0);
        int radius = Math.clamp(farthest + MARGIN, MIN_RADIUS, MAX_RADIUS);

        Set<ChunkPosition> chunks = new HashSet<>();
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                chunks.add(new ChunkPosition(x, z));
            }
        }
        return new SettlementBounds(center, chunks);
    }

    /**
     * Returns whether a claim, already clipped to the chunks no other settlement claims, holds enough chunks to found a
     * settlement on.
     *
     * @param minimumChunks the fewest chunks a founded settlement may claim
     */
    public static boolean isLargeEnough(SettlementBounds clipped, int minimumChunks) {
        return clipped.chunks().size() >= minimumChunks;
    }

}
