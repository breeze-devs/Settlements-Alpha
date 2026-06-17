package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialCueScriptTest {

    @Test
    void of_computesTotalDurationFromWaitsOnly() {
        // Arrange – two waits of 20 and 40 ticks, gesture and bubble contribute 0.
        List<CueStep> steps = List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(20)),
                new CueStep.Bubble("hi", ClockTicks.of(60)),
                new CueStep.Wait(ClockTicks.of(40))
        );

        // Act
        SocialCueScript script = SocialCueScript.of(steps);

        // Assert
        assertEquals(60L, script.getTotalDuration().getTicks());
    }

    @Test
    void stepAt_returnsCorrectStep() {
        // Arrange
        List<CueStep> steps = List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(20)),
                new CueStep.Gaze(null)
        );

        // Act
        SocialCueScript script = SocialCueScript.of(steps);

        // Assert – indices map to the original list order.
        assertEquals(steps.get(0), script.stepAt(0));
        assertEquals(steps.get(1), script.stepAt(1));
        assertEquals(steps.get(2), script.stepAt(2));
    }

    @Test
    void stepAt_throwsForOutOfBoundsIndex() {
        // Arrange
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Wait(ClockTicks.of(10))
        ));

        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> script.stepAt(5));
    }

    @Test
    void stepStartTick_nonWaitStepsFireAtSameOffsetAsPrecedingWait() {
        // Arrange
        // step 0 = Gesture (fires at t+0)
        // step 1 = Wait(20)  (fires at t+0, contributes 20 ticks)
        // step 2 = Bubble    (fires at t+20)
        // step 3 = Wait(30)  (fires at t+20, contributes 30 ticks)
        // step 4 = Gaze      (fires at t+50)
        long start = 1000L;
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gesture(AnimationArchetype.WAVE),
                new CueStep.Wait(ClockTicks.of(20)),
                new CueStep.Bubble("hi", ClockTicks.of(3)),
                new CueStep.Wait(ClockTicks.of(30)),
                new CueStep.Gaze(null)
        ));

        // Act & Assert
        assertEquals(1000L, script.stepStartTick(0, start)); // Gesture at start
        assertEquals(1000L, script.stepStartTick(1, start)); // Wait(20) at start
        assertEquals(1020L, script.stepStartTick(2, start)); // Bubble after Wait(20)
        assertEquals(1020L, script.stepStartTick(3, start)); // Wait(30) same offset as Bubble
        assertEquals(1050L, script.stepStartTick(4, start)); // Gaze after Wait(30)
    }

    @Test
    void stepCount_matchesListSize() {
        // Arrange
        SocialCueScript script = SocialCueScript.of(List.of(
                new CueStep.Gesture(AnimationArchetype.IDLE),
                new CueStep.Wait(ClockTicks.of(5)),
                new CueStep.Wait(ClockTicks.of(5))
        ));

        // Act & Assert
        assertEquals(3, script.stepCount());
    }

    @Test
    void of_emptyStepsProducesZeroDuration() {
        // Arrange & Act
        SocialCueScript script = SocialCueScript.of(List.of());

        // Assert
        assertEquals(0L, script.getTotalDuration().getTicks());
        assertEquals(0, script.stepCount());
    }

}
