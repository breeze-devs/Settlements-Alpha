package dev.breezes.settlements.domain.settlement.founding;

import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.world.ChunkPosition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim.MARGIN;
import static dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim.MAX_RADIUS;
import static dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim.MIN_RADIUS;
import static dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim.MIN_SQUARE_CHUNKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementFoundingClaimTest {

    private static final ChunkPosition CENTER = new ChunkPosition(-4, 9);

    @Test
    void square_isTheSmallestSquare_withNoPlacesToEnclose() {
        // Act
        SettlementBounds claim = SettlementFoundingClaim.square(CENTER, List.of());

        // Assert — its corners lie at the smallest radius, and it holds the count the configured minimum is capped at
        assertEquals(CENTER, claim.center());
        assertTrue(claim.contains(offset(MIN_RADIUS, MIN_RADIUS)));
        assertTrue(claim.contains(offset(-MIN_RADIUS, -MIN_RADIUS)));
        assertFalse(claim.contains(offset(MIN_RADIUS + 1, 0)));
        assertEquals(MIN_SQUARE_CHUNKS, claim.chunks().size());
    }

    @Test
    void square_staysTheSmallestSquare_whenThePlacesLieWellInsideIt() {
        // Arrange — the margin past this place still falls within the smallest square
        ChunkPosition near = offset(MIN_RADIUS - MARGIN, 0);

        // Act
        SettlementBounds claim = SettlementFoundingClaim.square(CENTER, List.of(near));

        // Assert
        assertEquals(SettlementFoundingClaim.square(CENTER, List.of()), claim);
    }

    @Test
    void square_enclosesTheFarthestPlace_plusTheMargin_inEveryDirection() {
        // Arrange — the far place sits on a diagonal, which is no farther than an edge in Chebyshev distance, and a
        // nearer place beside it must not decide the radius
        int farthest = MAX_RADIUS - MARGIN - 1;
        int radius = farthest + MARGIN;
        assertTrue(radius > MIN_RADIUS, "the case needs a radius the smallest square does not already reach");
        List<ChunkPosition> places = List.of(offset(1, 0), offset(-farthest, farthest));

        // Act
        SettlementBounds claim = SettlementFoundingClaim.square(CENTER, places);

        // Assert
        assertTrue(claim.contains(offset(-radius, radius)));
        assertTrue(claim.contains(offset(radius, -radius)));
        assertFalse(claim.contains(offset(radius + 1, 0)));
        assertFalse(claim.contains(offset(0, -radius - 1)));
    }

    @Test
    void square_stopsAtTheLargestRadius_whenAPlaceLiesFarther() {
        // Arrange
        ChunkPosition far = offset(0, MAX_RADIUS + MARGIN + 2);

        // Act
        SettlementBounds claim = SettlementFoundingClaim.square(CENTER, List.of(far));

        // Assert
        assertTrue(claim.contains(offset(MAX_RADIUS, MAX_RADIUS)));
        assertFalse(claim.contains(offset(0, MAX_RADIUS + 1)));
        assertFalse(claim.contains(far));
    }

    @Test
    void isLargeEnough_acceptsAClippedClaim_holdingExactlyTheMinimum() {
        // Arrange
        SettlementBounds clipped = clippedSquare();

        // Act, Assert
        assertTrue(SettlementFoundingClaim.isLargeEnough(clipped, clipped.chunks().size()));
    }

    @Test
    void isLargeEnough_refusesAClippedClaim_oneChunkShortOfTheMinimum() {
        // Arrange
        SettlementBounds clipped = clippedSquare();

        // Act, Assert
        assertFalse(SettlementFoundingClaim.isLargeEnough(clipped, clipped.chunks().size() + 1));
    }

    private static SettlementBounds clippedSquare() {
        // The smallest square with its eastern column claimed by another settlement
        SettlementBounds square = SettlementFoundingClaim.square(CENTER, List.of());
        Set<ChunkPosition> claimedElsewhere = Set.copyOf(square.chunks().stream()
                .filter(chunk -> chunk.x() == CENTER.x() + MIN_RADIUS)
                .toList());
        return square.clip(claimedElsewhere::contains).orElseThrow();
    }

    private static ChunkPosition offset(int dx, int dz) {
        return new ChunkPosition(CENTER.x() + dx, CENTER.z() + dz);
    }

}
