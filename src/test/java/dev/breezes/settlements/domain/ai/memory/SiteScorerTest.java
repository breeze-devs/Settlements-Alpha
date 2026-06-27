package dev.breezes.settlements.domain.ai.memory;

import org.junit.jupiter.api.Test;

import static dev.breezes.settlements.domain.ai.memory.DecayingSpatialObservationStoreTest.packPos;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SiteScorer#DISTANCE}.
 * <p>
 * Designed specifically to catch the X/Z swap bug that existed before {@link PackedPos} was introduced:
 * the old inline shifts assigned Z-bits to x and X-bits to z, so distance-ordering was wrong whenever
 * originX ≠ originZ.
 * <p>
 * Strategy: choose an origin with originX ≠ originZ, then place two sites so that the CORRECT
 * nearest site and the SWAPPED nearest site differ.  If X and Z were swapped, the scorer would
 * return the wrong ordering.  All arithmetic is on plain longs and ints — no Minecraft types.
 */
class SiteScorerTest {

    /**
     * Given an origin at (100, 64, 10):
     * <ul>
     *   <li>SITE_NEAR_X is close on X (x=101) but far on Z (z=50)</li>
     *   <li>SITE_NEAR_Z is far on X (x=150) but close on Z (z=11)</li>
     * </ul>
     * True distances from (100, 64, 10):
     * <ul>
     *   <li>SITE_NEAR_X → dx=1, dy=0, dz=40  → dist²=1+0+1600=1601</li>
     *   <li>SITE_NEAR_Z → dx=50, dy=0, dz=1  → dist²=2500+0+1=2501</li>
     * </ul>
     * SITE_NEAR_X must score lower (nearer).
     * <p>
     * With the OLD swapped extraction, the scorer would compute:
     * <ul>
     *   <li>SITE_NEAR_X: x_extracted=z_actual=50, z_extracted=x_actual=1
     *       → dx(50-100)²+dz(1-10)² = 2500+81 = 2581</li>
     *   <li>SITE_NEAR_Z: x_extracted=z_actual=11, z_extracted=x_actual=150
     *       → dx(11-100)²+dz(150-10)² = 7921+19600 = 27521</li>
     * </ul>
     * Both distances would be wrong but SITE_NEAR_X would still win, so we need a case where
     * the BUG flips the winner.
     * <p>
     * With asymmetric origin (originX=10, originZ=100) and:
     * <ul>
     *   <li>SITE_A at (11, 64, 200)  → true dx=1, dz=100 → 1+10000=10001</li>
     *   <li>SITE_B at (60, 64, 101)  → true dx=50, dz=1  → 2500+1=2501</li>
     * </ul>
     * Correct order: SITE_B (2501) before SITE_A (10001).
     * <p>
     * With SWAPPED extraction (x←z_bits, z←x_bits):
     * <ul>
     *   <li>SITE_A swapped: x_extr=200, z_extr=11 → (200-10)²+(11-100)² = 36100+7921=44021</li>
     *   <li>SITE_B swapped: x_extr=101, z_extr=60 → (101-10)²+(60-100)² = 8281+1600=9881</li>
     * </ul>
     * With swapped extraction SITE_B still wins (9881 < 44021) so this example also doesn't flip.
     * <p>
     * The definitive flip case: origin=(0, 64, 100), SITE_A at (5, 64, 90), SITE_B at (80, 64, 101).
     * <ul>
     *   <li>True: SITE_A → dx=5, dz=10 → 125; SITE_B → dx=80, dz=1 → 6401  → A wins</li>
     *   <li>Swapped: SITE_A x←90, z←5: (90-0)²+(5-100)²=8100+9025=17125;
     *                SITE_B x←101, z←80: (101-0)²+(80-100)²=10201+400=10601 → B wins (WRONG)</li>
     * </ul>
     * With correct extraction A wins; with the old swap B wins — this definitively catches the bug.
     */
    @Test
    void distanceScorer_ordersCorrectly_whenOriginXDiffersFromOriginZ() {
        // Arrange
        // Origin where originX ≠ originZ (0, 64, 100)
        int originX = 0;
        int originY = 64;
        int originZ = 100;

        // SITE_A is genuinely nearer: (5, 64, 90) → dx=5, dy=0, dz=10 → dist²=125
        long siteA = packPos(5, 64, 90);
        // SITE_B is farther: (80, 64, 101) → dx=80, dy=0, dz=1 → dist²=6401
        long siteB = packPos(80, 64, 101);

        long nowTick = 1000L;
        long lastSeenTick = 900L;

        // Act
        double scoreA = SiteScorer.DISTANCE.score(siteA, originX, originY, originZ, lastSeenTick, nowTick);
        double scoreB = SiteScorer.DISTANCE.score(siteB, originX, originY, originZ, lastSeenTick, nowTick);

        // Assert: SITE_A must score lower than SITE_B (it is nearer).
        // If X and Z were swapped during extraction, SITE_B would win — catching the old bug.
        assertTrue(scoreA < scoreB,
                "SITE_A (5, 64, 90) must score lower than SITE_B (80, 64, 101) from origin (0, 64, 100). " +
                        "If X/Z are swapped during extraction, B would incorrectly win. " +
                        "scoreA=" + scoreA + " scoreB=" + scoreB);
    }

    @Test
    void distanceScorer_returnsZero_forSiteAtOrigin() {
        // Arrange
        long siteAtOrigin = packPos(10, 64, 20);

        // Act
        double score = SiteScorer.DISTANCE.score(siteAtOrigin, 10, 64, 20, 0L, 0L);

        // Assert
        assertTrue(score == 0.0, "Site at the exact origin must score 0");
    }

    @Test
    void distanceScorer_extractsCorrectCoordinates_symmetricCase() {
        // Arrange: verify that extracted coords match what packPos encoded.
        // origin at (0, 0, 0), site at (3, 0, 4) → expected dist² = 9 + 0 + 16 = 25
        long site = packPos(3, 0, 4);

        // Act
        double score = SiteScorer.DISTANCE.score(site, 0, 0, 0, 0L, 0L);

        // Assert
        assertTrue(score == 25.0, "Expected dist² of 25 for (3,0,4) from (0,0,0), got " + score);
    }

}
