package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultUmbrellaAnimatorTest {

    private static final float DELTA = 0.0001F;
    private static final int ENTITY_ID = 1;
    private static final int NO_GATE_DELAY_TICKS = 0;
    // Short enough to keep the per-tick observation loops below readable; the mechanism does not vary with the magnitude.
    private static final int GATE_DELAY_TICKS = 8;

    @Test
    void isVisible_isFalseForAFreshlyCreatedAnimator() {
        // Arrange
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);

        // Act, Assert: a villager that has never seen rain carries no umbrella at all.
        assertFalse(animator.isVisible(context(0L, false)));
    }

    @Test
    void advance_appearsImmediatelyOncePastGateTurnsTrue() {
        // Arrange
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);

        // Act
        animator.advance(context(50L, true));

        // Assert: the attachment appears the same tick the gate flips, not after the carry clip finishes.
        assertTrue(animator.isVisible(context(50L, true)));
    }

    @Test
    void sample_armStartsMovingBeforeTheCanopyReachesItsOffsetTick() {
        // Arrange
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);
        animator.advance(context(0L, true));

        // Act: sample partway between raise start and the canopy's own offset tick, where the design
        // requires the arm already travelling while the canopy still holds its furled pose.
        int midTick = UmbrellaCarryAnimations.RAISE_CANOPY_START_TICK / 2;
        AnimationFrame frame = animator.sample(context(midTick, true));

        // Assert: the carry-raise arm tilt has already left its rest value...
        float armAtRest = UmbrellaCarryAnimations.raise().sample(0).get(AnimationTargets.ARMS_CROSSED_ROTATION).x();
        float armAtMidTick = frame.get(AnimationTargets.ARMS_CROSSED_ROTATION).x();
        assertTrue(Math.abs(armAtMidTick - armAtRest) > DELTA,
                "expected the carry-raise arm tilt to already be moving before the canopy's offset tick");

        // ...while the canopy is still exactly at its closed (furled) pose, not snapped open early.
        float closedEastRotation = UmbrellaAnimations.deploy().sample(0).get(UmbrellaAnimationTargets.EAST_ROTATION).z();
        float eastAtMidTick = frame.get(UmbrellaAnimationTargets.EAST_ROTATION).z();
        assertEquals(closedEastRotation, eastAtMidTick, DELTA,
                "expected the canopy to still be furled before its offset tick");
    }

    @Test
    void sample_canopyStartsOpeningOncePastItsOffsetTick() {
        // Arrange
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);
        animator.advance(context(0L, true));

        // Act: sample a couple ticks past the canopy's start offset.
        int pastOffsetTick = UmbrellaCarryAnimations.RAISE_CANOPY_START_TICK + 2;
        AnimationFrame frame = animator.sample(context(pastOffsetTick, true));

        // Assert
        float closedEastRotation = UmbrellaAnimations.deploy().sample(0).get(UmbrellaAnimationTargets.EAST_ROTATION).z();
        float eastPastOffsetTick = frame.get(UmbrellaAnimationTargets.EAST_ROTATION).z();
        assertTrue(Math.abs(eastPastOffsetTick - closedEastRotation) > DELTA,
                "expected the canopy to have started opening past its offset tick, but it still read the closed pose");
    }

    @Test
    void isVisible_survivesTheWholeLowerAndUndeploySequenceBeforeDisappearing() {
        // Arrange: raise fully, then let the gate drop from a fully-deployed hold.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);
        animator.advance(context(0L, true));
        long fullyDeployedGameTime = UmbrellaCarryAnimations.RAISE_DURATION_TICKS + 100L;
        assertTrue(animator.isVisible(context(fullyDeployedGameTime, true)));

        // Act
        animator.advance(context(fullyDeployedGameTime, false));

        // Assert: still visible for the whole lower-and-undeploy, and only gone once it has fully finished.
        long justBeforeFinished = fullyDeployedGameTime + UmbrellaCarryAnimations.LOWER_DURATION_TICKS - 1;
        long finished = fullyDeployedGameTime + UmbrellaCarryAnimations.LOWER_DURATION_TICKS;
        assertTrue(animator.isVisible(context(justBeforeFinished, false)),
                "expected the umbrella to remain visible until the lower-and-undeploy sequence fully finishes");
        assertFalse(animator.isVisible(context(finished, false)),
                "expected the umbrella to disappear once both the carry and canopy clips finish retracting");
    }

    @Test
    void advance_restartsTheOppositeClipWhenInterruptedMidRaise() {
        // Arrange: begin raising, and let it travel a quarter of the way up, so the interrupt lands on a
        // pose the raise is visibly partway through rather than on either endpoint.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);
        animator.advance(context(0L, true));
        long interruptTick = UmbrellaCarryAnimations.RAISE_DURATION_TICKS / 4;

        // Act: rain stops mid-raise.
        animator.advance(context(interruptTick, false));

        // Assert: the lower plays from its own first frame rather than from the pose the raise had
        // reached. The resulting snap is deliberate -- DefaultUmbrellaAnimator's Javadoc carries why.
        float lowerFirstFrameArm = UmbrellaCarryAnimations.lower().sample(0).get(AnimationTargets.ARMS_CROSSED_ROTATION).x();
        float armAfterInterrupt = animator.sample(context(interruptTick, false))
                .get(AnimationTargets.ARMS_CROSSED_ROTATION).x();
        assertEquals(lowerFirstFrameArm, armAfterInterrupt, DELTA,
                "expected an interrupted raise to restart the lower from its first frame");

        // The reversed motion must actually finish retracting in bounded time rather than stalling or
        // resuming the raise.
        assertFalse(animator.isVisible(context(interruptTick + UmbrellaCarryAnimations.LOWER_DURATION_TICKS + 1, false)),
                "expected the interrupted raise to finish retracting rather than never completing");
    }

    @Test
    void advance_isIdempotentWhenTheSameContextIsObservedTwiceInOneTick() {
        // Arrange: a supplementary render pass re-enters every render callback for one frame (platform
        // standard P8), so advance() must tolerate a second observation of the same context without
        // progressing the state machine any further than a single observation would.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(NO_GATE_DELAY_TICKS);
        UmbrellaAnimationContext context = context(7L, 0.4F, true);
        animator.advance(context);
        AnimationFrame afterFirstAdvance = animator.sample(context);
        boolean visibleAfterFirst = animator.isVisible(context);

        // Act: a second observation of the exact same context, as a supplementary pass would produce.
        animator.advance(context);
        AnimationFrame afterSecondAdvance = animator.sample(context);
        boolean visibleAfterSecond = animator.isVisible(context);

        // Assert
        assertEquals(afterFirstAdvance.get(AnimationTargets.ARMS_CROSSED_ROTATION),
                afterSecondAdvance.get(AnimationTargets.ARMS_CROSSED_ROTATION));
        assertEquals(afterFirstAdvance.get(UmbrellaAnimationTargets.EAST_ROTATION),
                afterSecondAdvance.get(UmbrellaAnimationTargets.EAST_ROTATION));
        assertEquals(visibleAfterFirst, visibleAfterSecond);
    }

    @Test
    void advance_holdsAGateChangeUntilTheGateDelayElapses() {
        // Arrange
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(GATE_DELAY_TICKS);

        // Act, Assert: the gate opens at tick 0 and stays open, and nothing moves until the whole delay
        // has been spent standing on it.
        for (long gameTime = 0; gameTime < GATE_DELAY_TICKS; gameTime++) {
            animator.advance(context(gameTime, true));
            assertFalse(animator.isVisible(context(gameTime, true)),
                    "expected the raise to still be held at tick " + gameTime + " of the gate delay");
        }

        animator.advance(context(GATE_DELAY_TICKS, true));
        assertTrue(animator.isVisible(context(GATE_DELAY_TICKS, true)),
                "expected the raise to commit on the tick the gate delay ran out");
    }

    @Test
    void advance_delaysTheLowerAsWellAsTheRaise() {
        // Arrange: raise fully and hold, so the lower starts from a settled pose rather than a reversal.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(GATE_DELAY_TICKS);
        advanceThrough(animator, 0L, GATE_DELAY_TICKS, true);
        long fullyDeployedGameTime = GATE_DELAY_TICKS + UmbrellaCarryAnimations.RAISE_DURATION_TICKS + 100L;

        // Act: the gate shuts from a fully-deployed hold.
        long undelayedStowGameTime = fullyDeployedGameTime + UmbrellaCarryAnimations.LOWER_DURATION_TICKS;
        advanceThrough(animator, fullyDeployedGameTime, undelayedStowGameTime, false);

        // Assert: still up at the tick an undelayed lower would already have finished stowing...
        assertTrue(animator.isVisible(context(undelayedStowGameTime, false)),
                "expected the lower to wait out the gate delay before it starts travelling");

        // ...and gone exactly one gate delay later than that.
        long delayedStowGameTime = undelayedStowGameTime + GATE_DELAY_TICKS;
        advanceThrough(animator, undelayedStowGameTime, delayedStowGameTime, false);
        assertFalse(animator.isVisible(context(delayedStowGameTime, false)),
                "expected the lower to finish a full gate delay behind an undelayed one");
    }

    @Test
    void advance_abandonsAGateChangeWhenTheGateReturnsBeforeTheDelayElapses() {
        // Arrange: a gate that opens and shuts again well inside the delay window -- the flicker the
        // delay exists to swallow.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(GATE_DELAY_TICKS);
        long blipEndGameTime = GATE_DELAY_TICKS / 2;
        advanceThrough(animator, 0L, blipEndGameTime - 1, true);

        // Act
        advanceThrough(animator, blipEndGameTime, GATE_DELAY_TICKS, false);

        // Assert: the umbrella never appears, including on the tick the abandoned change would have
        // committed had it survived.
        assertFalse(animator.isVisible(context(GATE_DELAY_TICKS, false)),
                "expected a gate blip shorter than the delay to leave the umbrella stowed");
    }

    @Test
    void advance_restartsTheGateDelayAfterAnAbandonedChange() {
        // Arrange: burn all but one tick of a delay window, then abandon it. A partly spent window that
        // survived would let a gate flickering on this cadence eventually commit on a very short run.
        DefaultUmbrellaAnimator animator = new DefaultUmbrellaAnimator(GATE_DELAY_TICKS);
        long abandonGameTime = GATE_DELAY_TICKS - 1;
        advanceThrough(animator, 0L, abandonGameTime - 1, true);
        animator.advance(context(abandonGameTime, false));

        // Act: the gate opens again on the very next tick.
        long reopenGameTime = abandonGameTime + 1;
        long recommitGameTime = reopenGameTime + GATE_DELAY_TICKS;
        advanceThrough(animator, reopenGameTime, recommitGameTime - 1, true);

        // Assert: it waited out a whole fresh window rather than finishing the leftover one.
        assertFalse(animator.isVisible(context(recommitGameTime - 1, true)),
                "expected the abandoned window to be discarded rather than resumed");
        animator.advance(context(recommitGameTime, true));
        assertTrue(animator.isVisible(context(recommitGameTime, true)),
                "expected the reopened gate to commit a full delay after it reopened");
    }

    /**
     * Feeds one observation per tick over the inclusive range, as a render pass observing every frame
     * does. A state machine driven by a delay only sees the tick it is handed, so a test that skips
     * ticks tests a cadence the renderer never produces.
     */
    private static void advanceThrough(DefaultUmbrellaAnimator animator,
                                       long fromGameTime,
                                       long toGameTime,
                                       boolean shouldDeploy) {
        for (long gameTime = fromGameTime; gameTime <= toGameTime; gameTime++) {
            animator.advance(context(gameTime, shouldDeploy));
        }
    }

    private static UmbrellaAnimationContext context(long gameTime, boolean shouldDeploy) {
        return context(gameTime, 0.0F, shouldDeploy);
    }

    private static UmbrellaAnimationContext context(long gameTime, float partialTicks, boolean shouldDeploy) {
        return new UmbrellaAnimationContext(ENTITY_ID, gameTime, partialTicks, shouldDeploy);
    }

}
