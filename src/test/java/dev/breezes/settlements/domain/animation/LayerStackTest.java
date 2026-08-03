package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerStackTest {

    private static final int DURATION_TICKS = 10;
    private static final int BLEND_OUT_TICKS = 4;
    private static final float ACTION_VALUE = 12.0F;

    @Test
    void clearAction_fadesTheActionOutRatherThanDroppingItOnTheSpot() {
        // Arrange
        LayerStack stack = new LayerStack(AnimationTestClips.empty("base"));
        stack.setSustainedAction(sustainedAction(), 0L);

        // Act
        stack.clearAction(0L);
        AnimationFrame halfwayOut = stack.sample(BLEND_OUT_TICKS / 2, 0.0F);

        // Assert
        assertEquals(ACTION_VALUE / 2.0F, halfwayOut.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertTrue(stack.hasAction());
    }

    @Test
    void clearAction_retiresTheLayerOnceTheFadeFinishes() {
        // Arrange
        LayerStack stack = new LayerStack(AnimationTestClips.empty("base"));
        stack.setSustainedAction(sustainedAction(), 0L);
        stack.clearAction(0L);

        // Act
        AnimationFrame afterFade = stack.sample(BLEND_OUT_TICKS, 0.0F);

        // Assert
        assertEquals(0.0F, afterFade.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertFalse(stack.hasAction());
    }

    @Test
    void clearAction_isHarmlessWithNoActionInFlight() {
        // Arrange
        LayerStack stack = new LayerStack(AnimationTestClips.empty("base"));

        // Act
        stack.clearAction(0L);

        // Assert
        assertFalse(stack.hasAction());
        assertEquals(0.0F, stack.sample(0L, 0.0F).get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void armConfiguration_fallsBackToRestOnceTheClearedActionRetires() {
        // Arrange
        LayerStack stack = new LayerStack(AnimationTestClips.empty("base"));
        stack.setSustainedAction(
                AnimationTestClips.armed("armed", DURATION_TICKS, BLEND_OUT_TICKS, ArmConfiguration.BOTH_STRAIGHT),
                0L);
        stack.clearAction(0L);

        // Act
        ArmConfiguration whileFading = stack.armConfiguration(BLEND_OUT_TICKS / 2, 0.0F);
        ArmConfiguration afterFade = stack.armConfiguration(BLEND_OUT_TICKS, 0.0F);

        // Assert: geometry cannot crossfade, so the action keeps its config for as long as it is present.
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, whileFading);
        assertEquals(ArmConfiguration.BOTH_CROSSED, afterFade);
    }

    @Test
    void fullLocomotionAdvancesIdleLifeWithoutSamplingItsHiddenFrameOrArmState() {
        // Arrange
        TrackingIdleLifeAnimator idleLifeAnimator = new TrackingIdleLifeAnimator();
        LayerStack stack = new LayerStack(
                AnimationTestClips.empty("base"),
                idleLifeAnimator,
                new FullWeightLocomotionAnimator(),
                1);

        // Act
        AnimationFrame frame = stack.sample(10L, 0.0F, LocomotionAnimationContext.idle());
        ArmConfiguration armConfiguration = stack.armConfiguration(
                10L, 0.0F, LocomotionAnimationContext.idle());

        // Assert
        assertEquals(0.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(2, idleLifeAnimator.advanceCount);
        assertEquals(0, idleLifeAnimator.sampleCount);
        assertEquals(0, idleLifeAnimator.armConfigurationCount);
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, armConfiguration);
    }

    private static KeyframeAnimation sustainedAction() {
        return AnimationTestClips.held("sustained", 0, DURATION_TICKS, BLEND_OUT_TICKS, ACTION_VALUE);
    }

    private static final class TrackingIdleLifeAnimator implements IdleLifeAnimator {

        private int advanceCount;
        private int sampleCount;
        private int armConfigurationCount;

        @Override
        public void advance(@Nonnull IdleLifeAnimationContext context) {
            this.advanceCount++;
        }

        @Override
        public AnimationFrame sample(@Nonnull IdleLifeAnimationContext context) {
            this.sampleCount++;
            return AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, ACTION_VALUE));
        }

        @Override
        public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull IdleLifeAnimationContext context) {
            this.armConfigurationCount++;
            return Optional.of(ArmConfiguration.BOTH_CROSSED);
        }

    }

    private static final class FullWeightLocomotionAnimator implements LocomotionAnimator {

        @Override
        public AnimationFrame sample(@Nonnull LocomotionAnimationContext context) {
            return AnimationFrame.EMPTY;
        }

        @Override
        public float weight(@Nonnull LocomotionAnimationContext context) {
            return 1.0F;
        }

        @Override
        public Optional<ArmConfiguration> activeArmConfiguration(@Nonnull LocomotionAnimationContext context) {
            return Optional.of(ArmConfiguration.BOTH_STRAIGHT);
        }

    }

}
