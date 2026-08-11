package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.domain.farming.CultivationZone;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CultivationLilySoundPalette#resizePitch} is the one pure decision in this palette — every
 * other method here plays a Minecraft sound, left for manual verification per this project's
 * established constraint on Minecraft in unit tests. Values are never asserted literally (F3, T2):
 * only the relationship a retune must preserve — pitch climbs across the resizable range, and the
 * wrap resets rather than continues the climb.
 */
class CultivationLilySoundPaletteTest {

    @Test
    void resizePitch_risesAcrossTheNonWrapRange() {
        // A defect that made pitch flat or inverted across the range would pass an equality check
        // against any single value, so the assertion has to be a chain of strict increases instead.
        float low = CultivationLilySoundPalette.resizePitch(CultivationZone.MIN_HALF_EXTENT + 1);
        float mid = CultivationLilySoundPalette.resizePitch(CultivationZone.MIN_HALF_EXTENT + 2);
        float high = CultivationLilySoundPalette.resizePitch(CultivationZone.MAX_HALF_EXTENT);

        assertTrue(low < mid, "pitch should rise from the low end of the range to the middle");
        assertTrue(mid < high, "pitch should keep rising from the middle to the top of the range");
    }

    @Test
    void resizePitch_wrapDropsBelowEveryOtherValueInRange() {
        // The wrap (landing back on MIN_HALF_EXTENT) must read as a reset to the floor, not as one
        // more step in the climb -- otherwise the boundary between "still growing" and "wrapped
        // around" is inaudible, which is the entire reason this sound exists (F3: the wrap must
        // produce a lower pitch than every other reachable value, not hit some specific number).
        float wrapPitch = CultivationLilySoundPalette.resizePitch(CultivationZone.MIN_HALF_EXTENT);

        for (int halfExtent = CultivationZone.MIN_HALF_EXTENT + 1; halfExtent <= CultivationZone.MAX_HALF_EXTENT; halfExtent++) {
            assertTrue(wrapPitch < CultivationLilySoundPalette.resizePitch(halfExtent),
                    "the wrap pitch must be lower than half-extent " + halfExtent + "'s pitch");
        }
    }

    @Test
    void resizePitch_maximumExtentReachesTheHighestPitchInRange() {
        float highest = CultivationLilySoundPalette.resizePitch(CultivationZone.MAX_HALF_EXTENT);

        for (int halfExtent = CultivationZone.MIN_HALF_EXTENT; halfExtent < CultivationZone.MAX_HALF_EXTENT; halfExtent++) {
            assertTrue(CultivationLilySoundPalette.resizePitch(halfExtent) <= highest,
                    "half-extent " + halfExtent + "'s pitch must not exceed the maximum extent's pitch");
        }
    }

    @Test
    void resizePitch_everyReachableSizeSoundsDistinct() {
        // Two sizes sharing a pitch makes the resize gesture unreadable by ear, which is the whole
        // point of the sound. It is also how a pitch table that has fallen behind a widened half-extent
        // range shows up: the lookup clamps rather than throwing, so the only visible symptom is the
        // top note repeating.
        Set<Float> distinctPitches = new HashSet<>();
        int reachableSizes = CultivationZone.MAX_HALF_EXTENT - CultivationZone.MIN_HALF_EXTENT + 1;

        for (int halfExtent = CultivationZone.MIN_HALF_EXTENT; halfExtent <= CultivationZone.MAX_HALF_EXTENT; halfExtent++) {
            distinctPitches.add(CultivationLilySoundPalette.resizePitch(halfExtent));
        }

        assertEquals(reachableSizes, distinctPitches.size(),
                "every reachable half-extent must sound at its own pitch");
    }

}
