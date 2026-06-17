package dev.breezes.settlements.application.ai.socialcue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Pure, stateless decision engine for cue advancement and completion.
 * <p>
 * Extracted from {@link SocialCueArbiter} so that the two critical timing questions —
 * "is it time to fire step N?" and "is the cue finished?" — can be reasoned about
 * and tested independently of the villager, the presenter, and Minecraft tick ordering.
 * <p>
 * Finish rule: the cue is done only when <em>both</em> conditions hold:
 * <ol>
 *   <li>All steps have been dispatched ({@code nextStepIndex >= stepCount}).</li>
 *   <li>Enough game-time has elapsed to cover the script's total duration
 *       ({@code gameTime >= cueStartGameTime + totalDurationTicks}).</li>
 * </ol>
 * Without the second gate, a trailing {@link CueStep.Wait} would be advanced past in the
 * same tick it was dispatched and {@code finish()} would immediately clear the gaze slot,
 * so the villager never actually turns toward the player.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SocialCueTimingPolicy {

    /**
     * Returns {@code true} if step {@code nextStepIndex} is due to fire at the given
     * {@code gameTime}. A step fires as soon as its scheduled start-tick has arrived.
     */
    public static boolean isStepDue(SocialCueScript script, int nextStepIndex, long cueStartGameTime, long gameTime) {
        long stepStart = script.stepStartTick(nextStepIndex, cueStartGameTime);
        return gameTime >= stepStart;
    }

    /**
     * Returns {@code true} when the active cue has run its full course and may be finished.
     */
    public static boolean isReadyToFinish(int nextStepIndex, int stepCount,
                                          long cueStartGameTime, long totalDurationTicks,
                                          long gameTime) {
        return nextStepIndex >= stepCount
                && gameTime >= (cueStartGameTime + totalDurationTicks);
    }

}
