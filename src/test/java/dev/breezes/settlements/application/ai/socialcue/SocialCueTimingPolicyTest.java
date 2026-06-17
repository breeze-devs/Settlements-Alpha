package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SocialCueTimingPolicy}.
 * <p>
 * Covers the two core decisions: when a step is due and when a cue is finished.
 * All inputs are plain Java values — no Minecraft objects involved.
 */
class SocialCueTimingPolicyTest {

    // =========================================================================
    // isStepDue
    // =========================================================================

    @Test
    void isStepDue_trueWhenGameTimeReachesStepStart() {
        // Arrange – step 0 fires at t=1000, step 1 (Wait 20) fires at t=1000, step 2 fires at t=1020.
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(20)),
                new CueStep.Bubble("hi", ClockTicks.of(3))
        ));
        long start = 1000L;

        // Act & Assert – step 2 is not yet due at t=1019.
        assertFalse(SocialCueTimingPolicy.isStepDue(script, 2, start, 1019L));
        // Exactly at t=1020 the step is due.
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 2, start, 1020L));
    }

    @Test
    void isStepDue_firstStepIsDueImmediately() {
        // Arrange
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gaze(null),
                new CueStep.Wait(ClockTicks.of(10))
        ));
        long start = 500L;

        // Act & Assert – step 0 starts at cueStart, so it is due at the admission tick.
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 0, start, start));
    }

    @Test
    void isStepDue_midScriptWaitGatesFollowingStep() {
        // Arrange – Gaze | Wait(15) | Gesture
        // step 0 = Gaze  → fires at t=0
        // step 1 = Wait  → fires at t=0, contributes 15 ticks
        // step 2 = Gesture → fires at t=15
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gaze(null),
                new CueStep.Wait(ClockTicks.of(15)),
                new CueStep.Gesture(AnimationArchetype.WAVE)
        ));
        long start = 0L;

        // Act & Assert
        // Step 2 (Gesture) must not fire before the 15-tick wait.
        assertFalse(SocialCueTimingPolicy.isStepDue(script, 2, start, 14L));
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 2, start, 15L));
    }

    // =========================================================================
    // isReadyToFinish — trailing Wait holds the cue active
    // =========================================================================

    @Test
    void isReadyToFinish_falseWhenStepsNotYetExhausted() {
        // Arrange – step 1 of 3 still pending; duration irrelevant.
        // Act & Assert
        assertFalse(SocialCueTimingPolicy.isReadyToFinish(1, 3, 0L, 40L, 100L));
    }

    @Test
    void isReadyToFinish_falseWhenStepsExhaustedButDurationNotElapsed() {
        // Arrange – steps done at t=1010 but 40-tick duration expires at t=1040.
        // This is exactly the trailing-Wait scenario: without the elapsed gate the
        // cue would finish in the same tick it dispatches the last step, destroying
        // gazeLookTarget before LookQueries can read it.
        // Act & Assert
        assertFalse(SocialCueTimingPolicy.isReadyToFinish(3, 3, 1000L, 40L, 1010L));
    }

    @Test
    void isReadyToFinish_trueOnlyWhenBothConditionsHold() {
        // Arrange
        long cueStart = 1000L;
        long totalDuration = 40L;
        int stepCount = 3;

        // Act & Assert – at exactly cueStart + totalDuration the cue can finish.
        assertTrue(SocialCueTimingPolicy.isReadyToFinish(stepCount, stepCount, cueStart, totalDuration, cueStart + totalDuration));
    }

    @Test
    void isReadyToFinish_trueAfterDurationElapses() {
        // Arrange – verify it also holds for any tick strictly past the boundary.
        long cueStart = 0L;
        long totalDuration = 20L;

        // Act & Assert
        assertTrue(SocialCueTimingPolicy.isReadyToFinish(2, 2, cueStart, totalDuration, 50L));
    }

    @Test
    void isReadyToFinish_zeroDurationScriptFinishesImmediately() {
        // Arrange – a script with no Wait steps has totalDuration=0; it should
        // finish as soon as steps are exhausted (same tick as admission).
        // Act & Assert
        assertTrue(SocialCueTimingPolicy.isReadyToFinish(2, 2, 500L, 0L, 500L));
    }

    // =========================================================================
    // End-to-end timeline: greet_player-style [Gaze, Gesture, Bubble, Wait(40)]
    // =========================================================================

    @Test
    void greetPlayerTimeline_gazeAndGestureFireImmediately_waitHoldsCueActive() {
        // Arrange – mirrors the greet_player catalog entry (Wait is ~40 ticks for this test).
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gaze(null),
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Bubble("hi", ClockTicks.of(3)),
                new CueStep.Wait(ClockTicks.of(40))
        ));
        long cueStart = 1000L;

        // Act & Assert – all three non-wait steps fire in the admission tick.
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 0, cueStart, cueStart)); // Gaze
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 1, cueStart, cueStart)); // Gesture
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 2, cueStart, cueStart)); // Bubble
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 3, cueStart, cueStart)); // Wait fires at cueStart too

        // All 4 steps dispatched, but the 40-tick duration has not yet elapsed —
        // cue must stay active so gazeLookTarget is not cleared yet.
        assertFalse(SocialCueTimingPolicy.isReadyToFinish(4, 4, cueStart, script.getTotalDuration().getTicks(), cueStart));
        assertFalse(SocialCueTimingPolicy.isReadyToFinish(4, 4, cueStart, script.getTotalDuration().getTicks(), cueStart + 39L));

        // At cueStart + 40 the duration has elapsed — now the cue may finish.
        assertTrue(SocialCueTimingPolicy.isReadyToFinish(4, 4, cueStart, script.getTotalDuration().getTicks(), cueStart + 40L));
    }

    @Test
    void midScriptWait_gatesFollowingStepAndDoesNotPreventFinish() {
        // Arrange – Wait in the middle: [Gesture, Wait(10), Gaze]
        // Total duration = 10 ticks (only the Wait contributes).
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(10)),
                new CueStep.Gaze(null)
        ));
        long cueStart = 0L;

        // Step 0 (Gesture) and step 1 (Wait) fire at t=0.
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 0, cueStart, 0L));
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 1, cueStart, 0L));

        // Step 2 (Gaze) is gated by the Wait: not due at t=9.
        assertFalse(SocialCueTimingPolicy.isStepDue(script, 2, cueStart, 9L));
        assertTrue(SocialCueTimingPolicy.isStepDue(script, 2, cueStart, 10L));

        // After all 3 steps dispatched and duration elapsed, cue can finish.
        assertTrue(SocialCueTimingPolicy.isReadyToFinish(3, 3, cueStart, script.getTotalDuration().getTicks(), 10L));
    }

}
