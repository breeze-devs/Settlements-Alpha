package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIdleLifeAnimatorTest {

    private static final int BREATHE_DURATION_TICKS = 80;
    private static final float BREATHE_PEAK = 4.0F;
    private static final int FIDGET_DURATION_TICKS = 40;
    private static final int FIDGET_BLEND_OUT_TICKS = 4;
    private static final float FIDGET_VALUE = 12.0F;

    /**
     * Comfortably past the longest randomized first-fidget delay, so a fidget is guaranteed to be
     * eligible without the test having to know what that delay is.
     */
    private static final long WELL_PAST_FIRST_FIDGET_DELAY = 20_000L;

    private static final int POPULATION_SIZE = 16;

    @Test
    void sample_doesNotPutEveryVillagerOnTheSameBreathingBeat() {
        // Arrange: one settlement's worth of villagers, all sampled at the same instant.
        Set<Float> observedPhases = new HashSet<>();

        // Act
        for (int entityId = 0; entityId < POPULATION_SIZE; entityId++) {
            DefaultIdleLifeAnimator animator = animator(entityId, List.of());
            AnimationFrame frame = advanceAndSample(animator, context(entityId, 0L, false));
            observedPhases.add(frame.get(AnimationTestTargets.FLOAT));
        }

        // Assert
        assertTrue(observedPhases.size() > 1,
                "villagers sampled at one instant all breathed in lockstep: " + observedPhases);
    }

    @Test
    void sample_keepsOneVillagerOnASteadyBreathingCycle() {
        // Arrange: the phase offset must be a fixed shift, not re-rolled per sample, or the loop stutters.
        DefaultIdleLifeAnimator animator = animator(7, List.of());

        // Act
        float firstCycle = advanceAndSample(animator, context(7, 0L, false)).get(AnimationTestTargets.FLOAT);
        float nextCycle = advanceAndSample(animator, context(7, BREATHE_DURATION_TICKS, false))
                .get(AnimationTestTargets.FLOAT);

        // Assert
        assertEquals(firstCycle, nextCycle, 0.0001F);
    }

    @Test
    void sample_fadesAnActiveFidgetOutWhenAnActionTakesOver() {
        // Arrange
        int entityId = 3;
        DefaultIdleLifeAnimator animator = animator(entityId, List.of(fidget()));
        long fidgetStart = WELL_PAST_FIRST_FIDGET_DELAY;
        float atFullStrength = fidgetContribution(animator, entityId, fidgetStart, false);

        // Act
        long actionStart = fidgetStart + 1;
        advanceAndSample(animator, context(entityId, actionStart, true));
        float partwayOut = fidgetContribution(animator, entityId, actionStart + FIDGET_BLEND_OUT_TICKS / 2, true);
        float fullyOut = fidgetContribution(animator, entityId, actionStart + FIDGET_BLEND_OUT_TICKS, true);

        // Assert
        assertEquals(FIDGET_VALUE, atFullStrength, 0.0001F);
        assertEquals(FIDGET_VALUE / 2.0F, partwayOut, 0.0001F);
        assertEquals(0.0F, fullyOut, 0.0001F);
    }

    @Test
    void sample_readsFidgetStateWithoutAdvancingIt() {
        // Arrange
        int entityId = 4;
        DefaultIdleLifeAnimator animator = animator(entityId, List.of(fidget()));
        IdleLifeAnimationContext context = context(entityId, WELL_PAST_FIRST_FIDGET_DELAY, false);

        // Act
        float beforeAdvance = animator.sample(context).get(AnimationTestTargets.OTHER_FLOAT);
        animator.advance(context);
        float afterAdvance = animator.sample(context).get(AnimationTestTargets.OTHER_FLOAT);
        float repeatedSample = animator.sample(context).get(AnimationTestTargets.OTHER_FLOAT);

        // Assert
        assertEquals(0.0F, beforeAdvance, 0.0001F);
        assertEquals(FIDGET_VALUE, afterAdvance, 0.0001F);
        assertEquals(afterAdvance, repeatedSample, 0.0001F);
    }

    @Test
    void sample_doesNotStartAFidgetWhileAnActionIsActive() {
        // Arrange
        int entityId = 5;
        DefaultIdleLifeAnimator animator = animator(entityId, List.of(fidget()));

        // Act
        float contribution = fidgetContribution(animator, entityId, WELL_PAST_FIRST_FIDGET_DELAY, true);

        // Assert
        assertEquals(0.0F, contribution, 0.0001F);
    }

    private static float fidgetContribution(DefaultIdleLifeAnimator animator,
                                            int entityId,
                                            long gameTime,
                                            boolean actionActive) {
        return advanceAndSample(animator, context(entityId, gameTime, actionActive))
                .get(AnimationTestTargets.OTHER_FLOAT);
    }

    private static AnimationFrame advanceAndSample(DefaultIdleLifeAnimator animator,
                                                   IdleLifeAnimationContext context) {
        animator.advance(context);
        return animator.sample(context);
    }

    private static DefaultIdleLifeAnimator animator(int entityId, List<KeyframeAnimation> fidgets) {
        return new DefaultIdleLifeAnimator(new StubLibrary(fidgets), entityId);
    }

    private static IdleLifeAnimationContext context(int entityId, long gameTime, boolean actionActive) {
        return new IdleLifeAnimationContext(entityId, gameTime, 0.0F, actionActive);
    }

    private static KeyframeAnimation fidget() {
        return AnimationTestClips.held(
                "fidget", 0, FIDGET_DURATION_TICKS, FIDGET_BLEND_OUT_TICKS, AnimationTestTargets.OTHER_FLOAT, FIDGET_VALUE);
    }

    /**
     * Breathes on one target and fidgets on another so a test can read either contribution on its own.
     */
    private record StubLibrary(List<KeyframeAnimation> fidgets) implements IdleLifeAnimationLibrary {

        @Override
        public KeyframeAnimation baseIdle() {
            return AnimationTestClips.ramping(
                    "breathe", BREATHE_DURATION_TICKS, AnimationTestTargets.FLOAT, BREATHE_PEAK);
        }

        @Override
        public KeyframeAnimation blink() {
            return AnimationTestClips.empty("blink");
        }

        @Override
        public List<KeyframeAnimation> fidgets() {
            return this.fidgets;
        }

    }

}
