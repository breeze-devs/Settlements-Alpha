package dev.breezes.settlements.domain.farming;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationZoneTest {

    private static final BlockPos LILY_POS = new BlockPos(10, 70, 10);

    @Test
    void streamCells_excludesCenterCell() {
        // An implementation that forgot the center filter would treat the water source itself as
        // a workable cell.
        CultivationZone zone = new CultivationZone(LILY_POS, 1, 1);

        List<BlockPos> cells = zone.streamCells().toList();

        assertFalse(cells.contains(LILY_POS.below()), "The water source directly below the lily must not appear as a cell");
    }

    @Test
    void streamCells_yieldsExpectedCellCount() {
        // A grid of (2*hx+1) by (2*hz+1) minus the excluded center cell — an off-by-one in the
        // range bounds would silently drop or duplicate an edge row or column.
        CultivationZone zone = new CultivationZone(LILY_POS, 2, 3);

        long count = zone.streamCells().count();

        assertEquals((2 * 2 + 1) * (2 * 3 + 1) - 1, count);
    }

    @Test
    void streamCells_allCellsSitOneBelowTheLily() {
        // The zone plane is the water's Y level, one below the lily-pad block; a cell computed at
        // the lily's own Y (or the canopy's) would place the farmer on the wrong floor entirely.
        CultivationZone zone = new CultivationZone(LILY_POS, 2, 2);

        boolean allAtWaterY = zone.streamCells().allMatch(pos -> pos.getY() == LILY_POS.getY() - 1);

        assertTrue(allAtWaterY);
    }

    @Test
    void streamCells_coversFullExtentInBothDirections() {
        // Regression guard for a range that only covers one side of the center (e.g. 0..hx instead
        // of -hx..hx), which would silently halve the zone.
        CultivationZone zone = new CultivationZone(LILY_POS, 1, 1);

        Set<BlockPos> cells = Set.copyOf(zone.streamCells().toList());

        assertTrue(cells.contains(LILY_POS.offset(-1, -1, -1)));
        assertTrue(cells.contains(LILY_POS.offset(1, -1, 1)));
    }

    @Test
    void constructor_extentAboveMax_clampsToMax() {
        // Simulates a corrupted or hand-edited save value above the valid range.
        CultivationZone zone = new CultivationZone(LILY_POS, CultivationZone.MAX_HALF_EXTENT + 5, 1);

        assertEquals(CultivationZone.MAX_HALF_EXTENT, zone.halfExtentX());
    }

    @Test
    void constructor_extentBelowMin_clampsToMin() {
        CultivationZone zone = new CultivationZone(LILY_POS, 0, 1);

        assertEquals(CultivationZone.MIN_HALF_EXTENT, zone.halfExtentX());
    }

    @Test
    void withNextHalfExtentX_belowMax_incrementsByOne() {
        CultivationZone zone = new CultivationZone(LILY_POS, CultivationZone.MIN_HALF_EXTENT, 1);

        CultivationZone resized = zone.withNextHalfExtentX();

        assertEquals(CultivationZone.MIN_HALF_EXTENT + 1, resized.halfExtentX());
    }

    @Test
    void withNextHalfExtentX_atMax_wrapsToMin() {
        // A clamp instead of a wrap here would leave a resize at the ceiling permanently inert —
        // repeated presses would change nothing.
        CultivationZone zone = new CultivationZone(LILY_POS, CultivationZone.MAX_HALF_EXTENT, 1);

        CultivationZone resized = zone.withNextHalfExtentX();

        assertEquals(CultivationZone.MIN_HALF_EXTENT, resized.halfExtentX());
    }

    @Test
    void withNextHalfExtentX_leavesPositionAndOtherAxisUnchanged() {
        // Fix-by-construction check: cycling one axis must not perturb the lily's own position or
        // the other axis's extent.
        CultivationZone zone = new CultivationZone(LILY_POS, 2, 3);

        CultivationZone resized = zone.withNextHalfExtentX();

        assertEquals(LILY_POS, resized.lilyPos());
        assertEquals(3, resized.halfExtentZ());
    }

    @Test
    void withNextHalfExtentZ_belowMax_incrementsByOne() {
        CultivationZone zone = new CultivationZone(LILY_POS, 1, CultivationZone.MIN_HALF_EXTENT);

        CultivationZone resized = zone.withNextHalfExtentZ();

        assertEquals(CultivationZone.MIN_HALF_EXTENT + 1, resized.halfExtentZ());
    }

    @Test
    void withNextHalfExtentZ_atMax_wrapsToMin() {
        CultivationZone zone = new CultivationZone(LILY_POS, 1, CultivationZone.MAX_HALF_EXTENT);

        CultivationZone resized = zone.withNextHalfExtentZ();

        assertEquals(CultivationZone.MIN_HALF_EXTENT, resized.halfExtentZ());
    }

    @Test
    void withNextHalfExtentZ_leavesPositionAndOtherAxisUnchanged() {
        CultivationZone zone = new CultivationZone(LILY_POS, 2, 3);

        CultivationZone resized = zone.withNextHalfExtentZ();

        assertEquals(LILY_POS, resized.lilyPos());
        assertEquals(2, resized.halfExtentX());
    }

}
