package dev.breezes.settlements.infrastructure.rendering.zone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneWallRendererTest {

    private static final float EPSILON_F = 1e-6F;

    @Test
    void wallAlpha_isSolidAtTheLineAndGoneAtTheOuterEdges() {
        // An inverted axis here costs the effect its entire point: the boundary line is the fact the wall
        // states, and a wall solid at its outer edges instead states it half a block away from where the
        // area ends — twice, since both halves read this one curve.
        assertEquals(1.0F, ZoneWallRenderer.wallAlpha(0.0D), EPSILON_F);
        assertEquals(0.0F, ZoneWallRenderer.wallAlpha(1.0D), EPSILON_F);
    }

    @Test
    void wallAlpha_holdsNearSolidCloseToTheLineThenFallsAwayQuickly() {
        // The shape, not the curve's exact identity: an exponent of 1 satisfies the endpoints above while
        // reading as an even ramp, which loses the solid line at the center.
        float dropNearestTheLine = ZoneWallRenderer.wallAlpha(0.0D) - ZoneWallRenderer.wallAlpha(0.25D);
        float dropNearestTheEdge = ZoneWallRenderer.wallAlpha(0.75D) - ZoneWallRenderer.wallAlpha(1.0D);

        assertTrue(dropNearestTheEdge > dropNearestTheLine * 4.0F,
                "Fade must accelerate with distance, not run evenly: nearest quarter dropped " + dropNearestTheLine
                        + ", outermost quarter dropped " + dropNearestTheEdge);
    }

    @Test
    void wallAlpha_fallsMonotonicallyAcrossEverySampledBandEdge() {
        // The geometry samples this at the band boundaries; a non-monotonic curve there would draw slices
        // that brighten with distance, reading as stacked stripes rather than one fading wall.
        float previous = ZoneWallRenderer.wallAlpha(0.0D);
        for (int band = 1; band <= 12; band++) {
            float current = ZoneWallRenderer.wallAlpha((double) band / 12);
            assertTrue(current < previous, "Alpha rose away from the line at band edge " + band);
            previous = current;
        }
    }

    @Test
    void pulseMultiplier_staysWithinTheConfiguredBrightnessRange() {
        // A dropped clamp or an amplitude/offset slip here would let the wall flash fully opaque or
        // swing into negative alpha rather than staying a gentle breathing effect.
        for (long tick = 0; tick < ZoneWallRenderer.PULSE_PERIOD_TICKS; tick++) {
            float multiplier = ZoneWallRenderer.pulseMultiplier(tick, 0.0F);
            assertTrue(multiplier >= ZoneWallRenderer.PULSE_MIN_MULTIPLIER - EPSILON_F,
                    "Pulse dipped below its configured floor at tick " + tick);
            assertTrue(multiplier <= ZoneWallRenderer.PULSE_MAX_MULTIPLIER + EPSILON_F,
                    "Pulse exceeded its configured ceiling at tick " + tick);
        }
    }

    @Test
    void pulseMultiplier_isContinuousAcrossThePeriodWrap() {
        // Regression coverage for a period reduction that lands mid-cycle: that defect makes the wave
        // jump backwards every time game time wraps past the period, reading as a visible stutter.
        long period = ZoneWallRenderer.PULSE_PERIOD_TICKS;
        float justBeforeWrap = ZoneWallRenderer.pulseMultiplier(period - 1, 0.999F);
        float justAfterWrap = ZoneWallRenderer.pulseMultiplier(period, 0.0F);

        assertEquals(justBeforeWrap, justAfterWrap, 0.01F, "Pulse must not jump at the period wrap");
    }

}
