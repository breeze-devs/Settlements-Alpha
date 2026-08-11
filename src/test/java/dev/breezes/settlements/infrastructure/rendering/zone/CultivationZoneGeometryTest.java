package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.domain.farming.CultivationZone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationZoneGeometryTest {

    private static final double EPSILON = 1e-9;

    @Test
    void canopyPlane_spansFullBlockWidthNotJustCellOrigins() {
        // A box built from the cell origins alone (no +1 on the max side) would end one block short
        // of the zone's true footprint — the outline would clip the far edge of the last cell.
        CultivationZone zone = new CultivationZone(new BlockPos(10, 70, 10), 1, 1);

        AABB box = CultivationZoneGeometry.canopyPlane(zone);

        assertEquals(9.0D, box.minX, EPSILON);
        assertEquals(12.0D, box.maxX, EPSILON);
        assertEquals(9.0D, box.minZ, EPSILON);
        assertEquals(12.0D, box.maxZ, EPSILON);
    }

    @Test
    void canopyPlane_sitsAboveTheWaterPlaneAtTheCanopyLevel() {
        // The zone's cells are enumerated one block below the lily (the water plane). The boundary line —
        // the part of the wall that actually states where the plot ends — must not land there, or it reads
        // as marking the pond rather than the field. The haze mirrored around it may reach into either.
        BlockPos lilyPos = new BlockPos(10, 70, 10);
        CultivationZone zone = new CultivationZone(lilyPos, 1, 1);

        AABB box = CultivationZoneGeometry.canopyPlane(zone);

        assertTrue(box.minY > lilyPos.getY() - 1, "Boundary line must sit above the water plane, not on it");
        assertTrue(box.minY < lilyPos.getY() + 1, "Boundary line must not reach the canopy above the lily itself");
    }

    @Test
    void centerCellPlane_coversExactlyTheCellTheLilyLandsOn() {
        // One cell, not one point: a zero-width box emits degenerate quads that draw nothing, so a marker
        // built from the lily position alone would silently fail to appear.
        BlockPos lilyPos = new BlockPos(10, 70, -4);

        AABB box = CultivationZoneGeometry.centerCellPlane(lilyPos);

        assertEquals(10.0D, box.minX, EPSILON);
        assertEquals(11.0D, box.maxX, EPSILON);
        assertEquals(-4.0D, box.minZ, EPSILON);
        assertEquals(-3.0D, box.maxZ, EPSILON);
    }

    @Test
    void centerCellPlane_sharesThePlaneWithTheFootprintItSitsInside() {
        // The preview draws both at once. Two different heights would read as the marker floating above or
        // sinking below its own plot, which looks like a bug in the placement rather than in the drawing.
        BlockPos lilyPos = new BlockPos(0, 64, 0);
        CultivationZone zone = CultivationZone.atDefault(lilyPos);

        assertEquals(CultivationZoneGeometry.canopyPlane(zone).minY,
                CultivationZoneGeometry.centerCellPlane(lilyPos).minY, EPSILON);
    }

    @Test
    void centerCellPlane_staysInsideTheFootprintAtTheSmallestZone() {
        // The marker is drawn with its own reach and inset; if it ever reached the perimeter the two walls
        // would overlap and read as one thick smear. The smallest zone is where that is tightest.
        BlockPos lilyPos = new BlockPos(0, 64, 0);
        CultivationZone smallest = new CultivationZone(lilyPos, CultivationZone.MIN_HALF_EXTENT, CultivationZone.MIN_HALF_EXTENT);

        AABB footprint = CultivationZoneGeometry.canopyPlane(smallest);
        AABB center = CultivationZoneGeometry.centerCellPlane(lilyPos);

        assertTrue(center.minX > footprint.minX && center.maxX < footprint.maxX,
                "Center marker must sit strictly inside the footprint on X");
        assertTrue(center.minZ > footprint.minZ && center.maxZ < footprint.maxZ,
                "Center marker must sit strictly inside the footprint on Z");
    }

}
