package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DwellRevealTrackerTest {

    private static final float EPSILON = 0.001F;

    /**
     * A client frame, near enough. Every duration here is spent one frame at a time, because opacity
     * depends on how the tracker arrived at a moment rather than on the moment alone — a single jumped
     * observation would not exercise the ramp at all.
     */
    private static final long FRAME_MILLIS = 16L;

    /**
     * Long enough to clear the dwell and then run the fade in to completion, with slack: the ramp only
     * starts once the dwell is served, so a hold of exactly dwell-plus-window ends short of full opacity.
     */
    private static final long HOLD_TO_FULL_MILLIS =
            DwellRevealTracker.DWELL_THRESHOLD_MILLIS + DwellRevealTracker.FADE_WINDOW_MILLIS * 2;

    private static final ClockTicks LINGER = ClockTicks.seconds(1);
    private static final long LINGER_MILLIS = ClientMonotonicClock.millisIn(LINGER);

    @Test
    void observe_targetHeldShortOfTheThreshold_isNotRevealedAndPaintsNothing() {
        // Without the dwell, a crosshair sweeping across a crowd of candidates strobes every one of them.
        DwellRevealTracker tracker = new DwellRevealTracker();
        tracker.observe("lily-a", 0L);

        DwellReveal reveal = tracker.observe("lily-a", DwellRevealTracker.DWELL_THRESHOLD_MILLIS - 1);

        assertFalse(reveal.revealed());
        assertFalse(reveal.paints());
    }

    @Test
    void observe_targetHeldToTheThreshold_isRevealed() {
        // A strict '>' instead of '>=' would leave this exact-boundary frame unrevealed.
        DwellRevealTracker tracker = new DwellRevealTracker();
        tracker.observe("lily-a", 0L);

        assertTrue(tracker.observe("lily-a", DwellRevealTracker.DWELL_THRESHOLD_MILLIS).revealed());
    }

    @Test
    void observe_differentKeyReplacingARevealedTarget_restartsTheDwell() {
        // The regression this guards: a tracker that read presence as a plain boolean stayed revealed
        // across this exact transition, which is what let a crosshair panning across a crowd of distinct
        // targets read as one continuous, already-earned reveal rather than as a fresh target.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        assertFalse(tracker.observe("lily-b", HOLD_TO_FULL_MILLIS).revealed());
    }

    @Test
    void alpha_neverReachesFullOpacityInASingleFrame() {
        // The pop this rules out: opacity resolved from the current frame's phase alone jumps to full the
        // instant the dwell is cleared, and back to zero the instant the crosshair slips off.
        DwellRevealTracker tracker = new DwellRevealTracker();
        tracker.observe("lily-a", 0L);

        DwellReveal reveal = tracker.observe("lily-a", DwellRevealTracker.DWELL_THRESHOLD_MILLIS);

        assertTrue(reveal.alpha() < 1.0F, "Opacity must ramp rather than snap, got " + reveal.alpha());
    }

    @Test
    void alpha_targetHeldPastTheFadeWindow_reachesFullOpacity() {
        DwellRevealTracker tracker = new DwellRevealTracker();

        assertEquals(1.0F, holdUntilFullyRevealed(tracker, "lily-a"), EPSILON);
    }

    @Test
    void alpha_targetLost_decaysAcrossTheWindowRatherThanCutting() {
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        float midFade = advance(tracker, null, HOLD_TO_FULL_MILLIS, DwellRevealTracker.FADE_WINDOW_MILLIS / 2);
        float pastFade = advance(tracker, null, HOLD_TO_FULL_MILLIS + DwellRevealTracker.FADE_WINDOW_MILLIS,
                DwellRevealTracker.FADE_WINDOW_MILLIS);

        assertTrue(midFade > 0.0F && midFade < 1.0F, "Mid-window opacity must be partial, got " + midFade);
        assertEquals(0.0F, pastFade, EPSILON);
    }

    @Test
    void observe_singleFrameDropoutOfTheSameTarget_barelyDentsOpacityAndKeepsTheDwell() {
        // The reported glitch: a lily pad is a one-and-a-half-pixel-tall hit box, so aim a player is
        // holding still drops and re-acquires it between frames. Charging that a fresh dwell blanks the
        // reveal for a whole threshold and then snaps it back — one dropped frame amplified into a blink.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");
        tracker.observe(null, HOLD_TO_FULL_MILLIS);

        DwellReveal reacquired = tracker.observe("lily-a", HOLD_TO_FULL_MILLIS + FRAME_MILLIS);

        assertTrue(reacquired.revealed(), "A target re-acquired while still on screen must not re-dwell");
        assertTrue(reacquired.alpha() > 0.8F, "One dropped frame must barely dent opacity, got " + reacquired.alpha());
    }

    @Test
    void observe_targetAlternatingWithAnIntruderKey_holdsTheRevealThroughout() {
        // The remaining half of the reported snap, and the one a null-dropout grace does not cover: the
        // pick ray does not go absent between frames, it lands on the water below the pad or on a passing
        // mob. Treating each of those as a new target restarts the dwell every other frame, so the reveal
        // spends its life blanking and snapping back rather than ever settling.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        float alpha = 0.0F;
        boolean revealedOnEveryLilyFrame = true;
        for (int frame = 0; frame < 20; frame++) {
            long now = HOLD_TO_FULL_MILLIS + frame * FRAME_MILLIS;
            DwellReveal reveal = tracker.observe(frame % 2 == 0 ? "water-below" : "lily-a", now);
            if (frame % 2 == 1) {
                revealedOnEveryLilyFrame &= reveal.revealed();
            }
            alpha = reveal.alpha();
        }

        assertTrue(revealedOnEveryLilyFrame, "An intruder key must not cost the revealed target its dwell");
        assertTrue(alpha > 0.5F, "Alternating frames must wobble opacity, not drain it, got " + alpha);
    }

    @Test
    void observe_freshKeyServingItsDwell_paintsWithoutBeingRevealed() {
        // The pair a consumer has to read as two separate answers. Opacity is still up, because it belongs
        // to the target that earned it and is only now decaying; `revealed` is false, because the new key
        // has not earned anything. A consumer that adopts new content on sight alone, rather than on
        // `revealed`, paints the newcomer at the opacity its predecessor built up — the newcomer appears at
        // near-full brightness having served no dwell at all, which is the whole point of having one.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        DwellReveal reveal = tracker.observe("lily-b", HOLD_TO_FULL_MILLIS + FRAME_MILLIS);

        assertFalse(reveal.revealed(), "A key one frame into its dwell has not earned the reveal");
        assertTrue(reveal.paints(), "The previous target's opacity is still on screen and still decaying");
    }

    @Test
    void observe_intruderKeyHeldPastTheThreshold_takesTheRevealOver() {
        // The grace only covers jitter. A target the player genuinely settles on has to be able to win,
        // or panning between two adjacent lilies would leave the first one's reveal stuck on screen.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        float alpha = advance(tracker, "lily-b", HOLD_TO_FULL_MILLIS, HOLD_TO_FULL_MILLIS);

        assertTrue(tracker.observe("lily-b", HOLD_TO_FULL_MILLIS * 2).revealed());
        assertEquals(1.0F, alpha, EPSILON);
    }

    @Test
    void observe_sameTargetReturningAfterTheFadeCompleted_earnsTheDwellAgain() {
        // The grace is bounded by what is still on screen: once the reveal is fully gone, looking back is
        // a new look and pays for itself, or a target abandoned minutes ago would snap straight back.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");
        long fadedOutAt = HOLD_TO_FULL_MILLIS + DwellRevealTracker.FADE_WINDOW_MILLIS * 2;
        advance(tracker, null, HOLD_TO_FULL_MILLIS, DwellRevealTracker.FADE_WINDOW_MILLIS * 2);

        assertFalse(tracker.observe("lily-a", fadedOutAt).revealed());
    }

    @Test
    void observe_targetGoneWithinTheLinger_holdsFullOpacityWithoutResolvingAnythingNew() {
        DwellRevealTracker tracker = new DwellRevealTracker(LINGER);
        holdUntilFullyRevealed(tracker, "lily-a");

        DwellReveal reveal = tracker.observe(null, HOLD_TO_FULL_MILLIS + LINGER_MILLIS / 2);

        assertEquals(1.0F, reveal.alpha(), EPSILON);
        assertFalse(reveal.revealed(), "A lingering reveal must not read as the target still being there");
    }

    @Test
    void observe_pastTheLinger_fadesOutAsUsual() {
        DwellRevealTracker tracker = new DwellRevealTracker(LINGER);
        holdUntilFullyRevealed(tracker, "lily-a");

        float alpha = advance(tracker, null, HOLD_TO_FULL_MILLIS,
                LINGER_MILLIS + DwellRevealTracker.FADE_WINDOW_MILLIS * 2);

        assertEquals(0.0F, alpha, EPSILON);
    }

    @Test
    void observe_withoutALinger_startsFadingOnTheFrameTheTargetIsLost() {
        // The default, and what the crosshair text wants: text that outlives its target is in the way.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        float alpha = advance(tracker, null, HOLD_TO_FULL_MILLIS, DwellRevealTracker.FADE_WINDOW_MILLIS * 2);

        assertEquals(0.0F, alpha, EPSILON);
    }

    @Test
    void reset_afterAFullReveal_paintsNothingOnTheNextFrame() {
        // Session end. The next session's first frame must inherit neither the opacity nor the instants
        // this one left behind, and its clock can read lower than the instant reset saw.
        DwellRevealTracker tracker = new DwellRevealTracker();
        holdUntilFullyRevealed(tracker, "lily-a");

        tracker.reset();

        assertFalse(tracker.observe("lily-a", 0L).paints());
    }

    private static float holdUntilFullyRevealed(DwellRevealTracker tracker, Object targetKey) {
        return advance(tracker, targetKey, 0L, HOLD_TO_FULL_MILLIS);
    }

    /**
     * Observes one target key once per frame across the given duration, and reports the opacity the ramp
     * ends on.
     */
    private static float advance(DwellRevealTracker tracker, Object targetKey, long startMillis, long durationMillis) {
        float alpha = 0.0F;
        for (long elapsed = FRAME_MILLIS; elapsed <= durationMillis; elapsed += FRAME_MILLIS) {
            alpha = tracker.observe(targetKey, startMillis + elapsed).alpha();
        }
        return alpha;
    }

}
