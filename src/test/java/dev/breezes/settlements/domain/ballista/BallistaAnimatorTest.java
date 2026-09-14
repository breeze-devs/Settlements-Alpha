package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.animation.AnimationTarget;
import dev.breezes.settlements.domain.animation.BallistaAnimations;
import dev.breezes.settlements.domain.animation.KeyframeAnimation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.breezes.settlements.domain.animation.AnimationFrameAssertions.assertSamePose;
import static dev.breezes.settlements.domain.animation.AnimationFrameAssertions.targetsKeyedBy;

class BallistaAnimatorTest {

    private static final KeyframeAnimation WIND = BallistaAnimations.wind();
    private static final int WIND_LENGTH = WIND.getDurationTicks();

    private static final KeyframeAnimation FIRE = BallistaAnimations.fire();
    private static final int FIRE_LENGTH = FIRE.getDurationTicks();

    private static final List<AnimationTarget<?>> TARGETS = targetsKeyedBy(WIND, FIRE);

    private static final long STARTED_AT = 500L;

    // Fractions of a tick a frame may be drawn at, including one just short of the next tick
    private static final float[] PARTIAL_TICKS = {0.0F, 0.25F, 0.5F, 0.999F};

    // Ticks watched either side of a clip, so the held poses before and after it are drawn too
    private static final int MARGIN_TICKS = 5;

    // Ticks a client's wind clip still has to play when the server's firing reaches it
    private static final int WIND_TICKS_LEFT_WHEN_FIRED = 3;

    @Test
    void sample_followsTheWindClip_fromTheRestPoseThroughTheCockedPose_forAWindWatchedFromItsStart() {
        // Arrange
        BallistaAnimator animator = new BallistaAnimator();
        BallistaStateMachine state = BallistaStateMachine.unwound();

        // Act, Assert: stepped the way a client ticks, every frame lies on the clip's own timeline, held at rest before
        // the wind and on its last frame after it. Dropping to rest as the clip runs out, or drawing a stage's pose on
        // top of the clip, both leave that timeline
        for (long now = STARTED_AT - MARGIN_TICKS; now <= STARTED_AT + WIND_LENGTH + MARGIN_TICKS; now++) {
            if (now == STARTED_AT) {
                state = state.startWindingAt(now);
            }
            state = state.settledAt(now);
            animator.updateAnimationFromState(state);

            for (float partialTick : PARTIAL_TICKS) {
                float clipTick = Math.clamp(now + partialTick - STARTED_AT, 0.0F, WIND_LENGTH);
                assertSamePose(TARGETS, WIND.sample(clipTick), animator.sample(state, now, partialTick),
                        "tick " + now + " + " + partialTick);
            }
        }
    }

    @Test
    void sample_holdsTheCockedPose_forAWindWhoseClipRanOutBeforeTheStateWasSettled() {
        // Arrange: a state handed over unsettled after its clip has already run out
        BallistaAnimator animator = new BallistaAnimator();
        BallistaStateMachine unsettled = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);
        animator.updateAnimationFromState(unsettled);

        // Act
        AnimationFrame frame = animator.sample(unsettled, STARTED_AT + WIND_LENGTH + MARGIN_TICKS, 0.0F);

        // Assert: taking the stage as given would draw a winding machine with no clip left to play, which is the rest
        // pose
        assertSamePose(TARGETS, WIND.sample(WIND_LENGTH), frame, "after the clip ran out");
    }

    @Test
    void sample_followsTheFireClip_fromTheCockedPoseToTheRestPose_forAShotWatchedFromItsCommit() {
        // Arrange
        BallistaAnimator animator = new BallistaAnimator();
        BallistaStateMachine state = BallistaStateMachine.cocked();

        // Act, Assert: before the commit the cocked pose is held, which is the fire clip's first frame, and after the
        // clip the rest pose is held, which is its last. Holding the cocked pose under the clip or after it would draw
        // the recoil on top of a drawn machine, or snap it back to cocked once the shot has gone
        for (long now = STARTED_AT - MARGIN_TICKS; now <= STARTED_AT + FIRE_LENGTH + MARGIN_TICKS; now++) {
            if (now == STARTED_AT) {
                state = state.fireAt(now);
            }
            state = state.settledAt(now);
            animator.updateAnimationFromState(state);

            for (float partialTick : PARTIAL_TICKS) {
                float clipTick = Math.clamp(now + partialTick - STARTED_AT, 0.0F, FIRE_LENGTH);
                assertSamePose(TARGETS, FIRE.sample(clipTick), animator.sample(state, now, partialTick),
                        "tick " + now + " + " + partialTick);
            }
        }
    }

    @Test
    void sample_holdsTheRestPose_forAFiringWhoseClipRanOutBeforeTheStateWasSettled() {
        // Arrange
        BallistaAnimator animator = new BallistaAnimator();
        BallistaStateMachine unsettled = BallistaStateMachine.cocked().fireAt(STARTED_AT);
        animator.updateAnimationFromState(unsettled);

        // Act
        AnimationFrame frame = animator.sample(unsettled, STARTED_AT + FIRE_LENGTH + MARGIN_TICKS, 0.0F);

        // Assert: taking the stage as given could hold a firing machine anywhere but at rest once its clip is gone
        assertSamePose(TARGETS, AnimationFrame.EMPTY, frame, "after the clip ran out");
    }

    @Test
    void continuesWinding_whenCockedStateArrivesBeforeLocalClipFinishes() {
        // Arrange
        BallistaAnimator animator = new BallistaAnimator();
        animator.updateAnimationFromState(BallistaStateMachine.unwound().startWindingAt(STARTED_AT));
        BallistaStateMachine receivedState = BallistaStateMachine.cocked();
        long now = STARTED_AT + WIND_LENGTH / 2;
        float partialTick = 0.5F;

        // Act
        animator.updateAnimationFromState(receivedState);
        AnimationFrame frame = animator.sample(receivedState, now, partialTick);

        // Assert
        assertSamePose(TARGETS, WIND.sample(now - STARTED_AT + partialTick), frame,
                "an early cocked state must not skip the remaining winding motion");
    }

    @Test
    void continuesFiring_whenUnwoundStateArrivesBeforeLocalClipFinishes() {
        // Arrange
        BallistaAnimator animator = new BallistaAnimator();
        animator.updateAnimationFromState(BallistaStateMachine.cocked().fireAt(STARTED_AT));
        BallistaStateMachine receivedState = BallistaStateMachine.unwound();
        long now = STARTED_AT + FIRE_LENGTH / 2;
        float partialTick = 0.5F;

        // Act
        animator.updateAnimationFromState(receivedState);
        AnimationFrame frame = animator.sample(receivedState, now, partialTick);

        // Assert
        assertSamePose(TARGETS, FIRE.sample(now - STARTED_AT + partialTick), frame,
                "an early unwound state must not skip the remaining recoil");
    }

    @Test
    void updateAnimationFromState_playsTheFireClip_inPlaceOfAWindClipStillPlaying() {
        // Arrange: a client whose wind clip still has ticks left when the server's commit reaches it
        BallistaAnimator animator = new BallistaAnimator();
        long windStartedAt = STARTED_AT - WIND_LENGTH + WIND_TICKS_LEFT_WHEN_FIRED;
        animator.updateAnimationFromState(BallistaStateMachine.unwound().startWindingAt(windStartedAt));
        BallistaStateMachine firing = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act
        animator.updateAnimationFromState(firing);

        // Assert: an animator that ignored a new clip while one is playing would finish the wind instead, and then
        // hold the rest pose with the recoil never drawn
        for (long now = STARTED_AT; now <= STARTED_AT + FIRE_LENGTH; now++) {
            assertSamePose(TARGETS, FIRE.sample(now - STARTED_AT), animator.sample(firing, now, 0.0F),
                    "tick " + (now - STARTED_AT) + " of the firing");
        }
    }

}
