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

    private static final int FILLER_ENTITY_ID = 1;
    private static final int DURATION_TICKS = 10;
    private static final int BLEND_OUT_TICKS = 4;
    private static final float ACTION_VALUE = 12.0F;
    private static final float ACTION_ABSOLUTE_VALUE = 3.0F;
    private static final float UMBRELLA_ABSOLUTE_VALUE = 9.0F;

    @Test
    void clearAction_fadesTheActionOutRatherThanDroppingItOnTheSpot() {
        // Arrange
        LayerStack stack = emptyBaseStack();
        stack.setSustainedAction(sustainedAction(), 0L);

        // Act
        stack.clearAction(0L);
        AnimationFrame halfwayOut = sample(stack, BLEND_OUT_TICKS / 2, 0.0F);

        // Assert
        assertEquals(ACTION_VALUE / 2.0F, halfwayOut.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertTrue(stack.hasAction());
    }

    @Test
    void clearAction_retiresTheLayerOnceTheFadeFinishes() {
        // Arrange
        LayerStack stack = emptyBaseStack();
        stack.setSustainedAction(sustainedAction(), 0L);
        stack.clearAction(0L);

        // Act
        AnimationFrame afterFade = sample(stack, BLEND_OUT_TICKS, 0.0F);

        // Assert
        assertEquals(0.0F, afterFade.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertFalse(stack.hasAction());
    }

    @Test
    void clearAction_isHarmlessWithNoActionInFlight() {
        // Arrange
        LayerStack stack = emptyBaseStack();

        // Act
        stack.clearAction(0L);

        // Assert
        assertFalse(stack.hasAction());
        assertEquals(0.0F, sample(stack, 0L, 0.0F).get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void armConfiguration_fallsBackToRestOnceTheClearedActionRetires() {
        // Arrange
        LayerStack stack = emptyBaseStack();
        stack.setSustainedAction(
                AnimationTestClips.armed("armed", DURATION_TICKS, BLEND_OUT_TICKS, ArmConfiguration.BOTH_STRAIGHT),
                0L);
        stack.clearAction(0L);

        // Act
        ArmConfiguration whileFading = armConfiguration(stack, BLEND_OUT_TICKS / 2, 0.0F);
        ArmConfiguration afterFade = armConfiguration(stack, BLEND_OUT_TICKS, 0.0F);

        // Assert: geometry cannot crossfade, so the action keeps its config for as long as it is present.
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, whileFading);
        assertEquals(ArmConfiguration.BOTH_CROSSED, afterFade);
    }

    @Test
    void sample_composesTheUmbrellaOverTheWholeActionFold() {
        // Arrange: ABSOLUTE_FLOAT makes composition order observable -- the later frame's value replaces
        // rather than adds, so an action holding a different value proves placement, not just additive
        // stacking: the umbrella composes over the whole action fold.
        LayerStack stack = emptyBaseStack(LayerStackAnimators.builder()
                .umbrella(new ConstantUmbrellaAnimator(UMBRELLA_ABSOLUTE_VALUE))
                .build());
        stack.setSustainedAction(
                AnimationTestClips.held("action", 0, DURATION_TICKS, 0, AnimationTestTargets.ABSOLUTE_FLOAT, ACTION_ABSOLUTE_VALUE),
                0L);

        // Act
        AnimationFrame frame = sample(stack, 0L, 0.0F);

        // Assert
        assertEquals(UMBRELLA_ABSOLUTE_VALUE, frame.get(AnimationTestTargets.ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void fullLocomotionAdvancesIdleLifeWithoutSamplingItsHiddenFrameOrArmState() {
        // Arrange
        TrackingIdleLifeAnimator idleLifeAnimator = new TrackingIdleLifeAnimator();
        LayerStack stack = emptyBaseStack(LayerStackAnimators.builder()
                .idleLife(idleLifeAnimator)
                .locomotion(new FullWeightLocomotionAnimator())
                .build());

        // Act
        AnimationFrame frame = sample(stack, 10L, 0.0F);
        ArmConfiguration armConfiguration = armConfiguration(stack, 10L, 0.0F);

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

    private static LayerStack emptyBaseStack() {
        return emptyBaseStack(LayerStackAnimators.builder().build());
    }

    private static LayerStack emptyBaseStack(@Nonnull LayerStackAnimators animators) {
        return new LayerStack(AnimationTestClips.empty("base"), animators, FILLER_ENTITY_ID);
    }

    private static AnimationFrame sample(@Nonnull LayerStack stack, long gameTime, float partialTicks) {
        return stack.sample(gameTime, partialTicks, LocomotionAnimationContext.idle(), false);
    }

    private static ArmConfiguration armConfiguration(@Nonnull LayerStack stack, long gameTime, float partialTicks) {
        return stack.armConfiguration(gameTime, partialTicks, LocomotionAnimationContext.idle());
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

    private static final class ConstantUmbrellaAnimator implements UmbrellaAnimator {

        private final float value;

        private ConstantUmbrellaAnimator(float value) {
            this.value = value;
        }

        @Override
        public void advance(@Nonnull UmbrellaAnimationContext context) {
        }

        @Override
        public AnimationFrame sample(@Nonnull UmbrellaAnimationContext context) {
            return AnimationFrame.of(Map.of(AnimationTestTargets.ABSOLUTE_FLOAT, this.value));
        }

        @Override
        public boolean isVisible(@Nonnull UmbrellaAnimationContext context) {
            return true;
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
