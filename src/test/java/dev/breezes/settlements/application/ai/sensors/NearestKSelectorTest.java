package dev.breezes.settlements.application.ai.sensors;

import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NearestKSelector}.
 * All tests operate on packed longs without importing any Minecraft class.
 * <p>
 * Packed positions use the same bit layout as {@code BlockPos.asLong()}:
 * X in bits [63..38] (26 bits), Z in bits [37..12] (26 bits), Y in bits [11..0] (12 bits).
 */
class NearestKSelectorTest {

    // All positions sit at Y=64 and are centred on anchor (0, 64, 0).
    private static final int ANCHOR_X = 0;
    private static final int ANCHOR_Y = 64;
    private static final int ANCHOR_Z = 0;

    // Positions inside the query box (horizontal radius 10, vertical radius 5):
    private static final long POS_NEAR = packPos(1, 64, 1);   // dist²  = 2  (closest)
    private static final long POS_MID = packPos(5, 64, 5);   // dist²  = 50
    private static final long POS_FAR = packPos(9, 64, 9);   // dist²  = 162 (farthest in-box)

    // Position outside the box:
    private static final long POS_OUTSIDE = packPos(20, 64, 20); // |x|=20 > radius 10

    // -------------------------------------------------------------------------
    // More in-box hits than K → truncated=true, only K nearest kept
    // -------------------------------------------------------------------------

    @Test
    void whenHitsExceedK_keepsKNearestAndReportsTruncated() {
        // Arrange: three in-box positions, K=2. FAR should be dropped.
        List<long[]> chunks = List.of(new long[]{POS_NEAR, POS_MID, POS_FAR});

        // Act
        NearestKSelector.SelectionResult result = NearestKSelector.select(
                chunks, ANCHOR_X, ANCHOR_Y, ANCHOR_Z, 10, 5, 2);

        // Assert: truncated because 3 in-box hits > K=2; FAR dropped.
        assertTrue(result.truncated(), "truncated must be true when in-box count exceeds K");
        assertEquals(2, result.sites().size(), "exactly K sites must be returned");
        assertContains(result.sites(), POS_NEAR, "nearest site must be kept");
        assertContains(result.sites(), POS_MID, "second-nearest site must be kept");
        assertNotContains(result.sites(), POS_FAR, "farthest site must be evicted");
    }

    // -------------------------------------------------------------------------
    // Fewer or equal in-box hits than K → truncated=false, all kept
    // -------------------------------------------------------------------------

    @Test
    void whenHitsDoNotExceedK_keepsAllAndReportsNotTruncated() {
        // Arrange: two in-box positions, K=5.
        List<long[]> chunks = List.of(new long[]{POS_NEAR, POS_MID});

        // Act
        NearestKSelector.SelectionResult result = NearestKSelector.select(
                chunks, ANCHOR_X, ANCHOR_Y, ANCHOR_Z, 10, 5, 5);

        // Assert: not truncated because 2 in-box hits <= K=5.
        assertFalse(result.truncated(), "truncated must be false when in-box count does not exceed K");
        assertEquals(2, result.sites().size(), "all in-box sites must be returned");
        assertContains(result.sites(), POS_NEAR);
        assertContains(result.sites(), POS_MID);
    }

    // -------------------------------------------------------------------------
    // Out-of-box hits are excluded and do not count toward K or truncation
    // -------------------------------------------------------------------------

    @Test
    void outOfBoxHitsAreExcludedAndDoNotCountTowardTruncation() {
        // Arrange: one in-box hit, one out-of-box hit. K=1 — if the out-of-box hit counted,
        // truncated would be true; it must stay false.
        List<long[]> chunks = List.of(new long[]{POS_NEAR, POS_OUTSIDE});

        // Act
        NearestKSelector.SelectionResult result = NearestKSelector.select(
                chunks, ANCHOR_X, ANCHOR_Y, ANCHOR_Z, 10, 5, 1);

        // Assert: only the in-box hit is considered; K=1 so count==K, not truncated.
        assertFalse(result.truncated(), "out-of-box hits must not count toward truncation");
        assertEquals(1, result.sites().size());
        assertContains(result.sites(), POS_NEAR, "in-box near site must be kept");
        assertNotContains(result.sites(), POS_OUTSIDE, "out-of-box site must be excluded");
    }

    @Test
    void emptyInput_returnsEmptyNotTruncated() {
        // Arrange
        List<long[]> chunks = List.of();

        // Act
        NearestKSelector.SelectionResult result = NearestKSelector.select(
                chunks, ANCHOR_X, ANCHOR_Y, ANCHOR_Z, 10, 5, 32);

        // Assert
        assertFalse(result.truncated());
        assertTrue(result.sites().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Packs block coordinates into a long using the same bit layout as
     * {@code BlockPos.asLong(x, y, z)}, so the domain's bit-extraction logic is
     * tested without importing any Minecraft class.
     * <p>
     * Layout (same as vanilla): X in bits [63..38] (26 bits), Z in bits [37..12] (26 bits),
     * Y in bits [11..0] (12 bits), all values sign-extended within their respective widths.
     */
    static long packPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (long) (y & 0xFFF);
    }

    private static void assertContains(LongList sites, long packed) {
        assertTrue(sites.contains(packed), "Expected site " + packed + " to be present");
    }

    private static void assertContains(LongList sites, long packed, String message) {
        assertTrue(sites.contains(packed), message);
    }

    private static void assertNotContains(LongList sites, long packed, String message) {
        assertFalse(sites.contains(packed), message);
    }

}
