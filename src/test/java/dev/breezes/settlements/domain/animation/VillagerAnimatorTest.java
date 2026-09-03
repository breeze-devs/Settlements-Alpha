package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerAnimatorTest {

    private static final int FILLER_ENTITY_ID = 1;
    private static final float UMBRELLA_VALUE = 7.0F;

    @Test
    void onArchetypeChanged_startsNewAnimation() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, constantAnimation("swing", 12.0F, 0)));
        VillagerAnimator animator = animator(resolver);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 20L);
        AnimationFrame frame = sample(animator, 20L, 0.0F);

        // Assert
        assertEquals(AnimationArchetype.SWING_HEAVY, animator.getLastSeenArchetype());
        assertEquals(12.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_blendsOutgoingAndCurrentAnimationDuringCrossfade() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.INTERACT, constantAnimation("interact", 10.0F, 0),
                AnimationArchetype.SWING_HEAVY, constantAnimation("swing", 20.0F, 4)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 2, AnimationSelectionContext.generic(), 10L);
        AnimationFrame frame = sample(animator, 12L, 0.0F);

        // Assert
        assertEquals(15.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_blendsNewActionInFromUnderlyingLayers() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, constantAnimation("swing", 12.0F, 4)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 20L);

        // Act
        AnimationFrame frame = sample(animator, 22L, 0.0F);

        // Assert
        assertEquals(6.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_blendsSustainedActionOutWhenCleared() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.EAT, animation("eat", 0, 20, 4, 12.0F, null)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.EAT, (byte) 0, AnimationSelectionContext.generic(), 0L);
        animator.onMotionChanged(AnimationArchetype.IDLE, (byte) 0, AnimationSelectionContext.generic(), 10L);

        // Act
        AnimationFrame frame = sample(animator, 12L, 0.0F);

        // Assert
        assertEquals(6.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_snapsSleepOnAndOff() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SLEEP, animation("sleep", 4, 100, 4, 12.0F, null)));
        VillagerAnimator animator = animator(resolver);
        animator.setSleeping(true, 20L);

        // Act
        AnimationFrame enteringFrame = sample(animator, 22L, 0.0F);
        animator.setSleeping(false, 24L);
        AnimationFrame leavingFrame = sample(animator, 26L, 0.0F);

        // Assert
        assertEquals(12.0F, enteringFrame.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(0.0F, leavingFrame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_doesNotMixTheAwakePoseIntoSleep() {
        // Arrange: both clips drive the same target, but sleep is a discrete pose rather than a layer
        // transition and therefore owns the result immediately.
        int sleepBlendInTicks = 4;
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 10.0F, 0),
                AnimationArchetype.SLEEP, animation("sleep", sleepBlendInTicks, 100, 4, 20.0F, null)));
        VillagerAnimator animator = animator(resolver);
        animator.setSleeping(true, 0L);

        // Act
        AnimationFrame atSleepStart = sample(animator, 0L, 0.0F);
        AnimationFrame laterInSleep = sample(animator, sleepBlendInTicks, 0.0F);

        // Assert
        assertEquals(20.0F, atSleepStart.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(20.0F, laterInSleep.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_blendsMissingTargetsAgainstNeutralValue() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.INTERACT, constantAnimation("hold", 10.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 4, List.of())));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 2, AnimationSelectionContext.generic(), 10L);
        AnimationFrame frame = sample(animator, 12L, 0.0F);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_autoPopsTriggeredActionAfterDurationAndBlendOut() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 0, 10, 2, 12.0F, null)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 20L);

        // Act
        AnimationFrame frameDuringBlendOut = sample(animator, 31L, 0.0F);
        AnimationFrame frameAfterExpiry = sample(animator, 33L, 0.1F);

        // Assert
        assertEquals(6.0F, frameDuringBlendOut.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(0.0F, frameAfterExpiry.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_triggeredActionAfterSustainedStillAutoPops() {
        // Arrange: a one-shot triggered while a sustained loop is active must still auto-pop.
        // The action layer's lifetime follows the latest push, not the layer's original kind.
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.EAT, constantAnimation("eat", 10.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 0, 10, 0, 12.0F, null)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.EAT, (byte) 0, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 0L);
        AnimationFrame frame = sample(animator, 20L, 0.0F);

        // Assert
        assertEquals(0.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_sustainedActionAfterTriggeredDoesNotAutoPop() {
        // Arrange: a sustained loop set while a one-shot is still active must persist;
        // it must not inherit the one-shot's transient (auto-pop) lifetime.
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 0, 10, 0, 12.0F, null),
                AnimationArchetype.EAT, constantAnimation("eat", 10.0F, 0)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.EAT, (byte) 1, AnimationSelectionContext.generic(), 0L);
        AnimationFrame frame = sample(animator, 50L, 0.0F);

        // Assert
        assertEquals(10.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void currentArmConfiguration_fallsBackToBaseWhenActionDoesNotOwnArmConfiguration() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.INTERACT, animation("interact", 0, 10, 0, 12.0F, null)));
        VillagerAnimator animator = animator(resolver);

        // Act
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Assert
        assertEquals(ArmConfiguration.BOTH_CROSSED, armConfiguration(animator, 0L, 0.0F));
    }

    @Test
    void currentArmConfiguration_returnsActionOverrideUntilActionExpires() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.HARVEST, animation("harvest", 0, 10, 0, 12.0F, ArmConfiguration.BOTH_STRAIGHT)));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.HARVEST, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        ArmConfiguration activeConfig = armConfiguration(animator, 0L, 0.0F);
        sample(animator, 11L, 0.1F);

        // Assert
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, activeConfig);
        assertEquals(ArmConfiguration.BOTH_CROSSED, armConfiguration(animator, 0L, 0.0F));
    }

    @Test
    void currentArmConfiguration_samplesActionTimeline() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.HARVEST, timelineArmAnimation("harvest")));
        VillagerAnimator animator = animator(resolver);
        animator.onMotionChanged(AnimationArchetype.HARVEST, (byte) 1, AnimationSelectionContext.generic(), 10L);

        // Act, Assert
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, armConfiguration(animator, 29L, 0.9F));
        assertEquals(ArmConfiguration.BOTH_CROSSED, armConfiguration(animator, 30L, 0.0F));
    }

    @Test
    void sample_stowsTheUmbrellaWhenTheVillagerFallsAsleepStillHoldingIt() {
        // Arrange: deployed and visible while awake under an open gate.
        GatedUmbrellaAnimator umbrella = new GatedUmbrellaAnimator();
        VillagerAnimator animator = umbrellaAnimator(umbrella);
        animator.sample(0L, 0.0F, LocomotionAnimationContext.idle(), true);
        assertTrue(animator.isUmbrellaVisible(0L, 0.0F));

        // Act: the villager lies down while the weather gate is still open.
        animator.setSleeping(true, 10L);
        AnimationFrame sleepingFrame = animator.sample(10L, 0.0F, LocomotionAnimationContext.idle(), true);

        // Assert: sleep overrides the gate, and the umbrella stows rather than riding the sleep pose.
        assertFalse(animator.isUmbrellaVisible(10L, 0.0F));
        assertFalse(sleepingFrame.has(AnimationTestTargets.ABSOLUTE_FLOAT));
    }

    @Test
    void sample_composesTheUmbrellaOverTheSleepPoseWhileItIsStillRetracting() {
        // Arrange: an umbrella that stays visible through its retract, as the real one does for the
        // whole lower-and-close.
        VillagerAnimator animator = umbrellaAnimator(new AlwaysVisibleUmbrellaAnimator());
        animator.setSleeping(true, 0L);

        // Act
        AnimationFrame frame = animator.sample(0L, 0.0F, LocomotionAnimationContext.idle(), false);

        // Assert: the retract needs its own frame folded over the sleep pose, or the canopy snaps to its
        // default rather than closing.
        assertEquals(UMBRELLA_VALUE, frame.get(AnimationTestTargets.ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void sample_redeploysTheUmbrellaOnWakingWhileTheGateIsStillOpen() {
        // Arrange: slept through an open gate, so the umbrella is stowed.
        GatedUmbrellaAnimator umbrella = new GatedUmbrellaAnimator();
        VillagerAnimator animator = umbrellaAnimator(umbrella);
        animator.setSleeping(true, 0L);
        animator.sample(0L, 0.0F, LocomotionAnimationContext.idle(), true);

        // Act
        animator.setSleeping(false, 20L);
        animator.sample(20L, 0.0F, LocomotionAnimationContext.idle(), true);

        // Assert
        assertTrue(animator.isUmbrellaVisible(20L, 0.0F));
    }

    private static VillagerAnimator umbrellaAnimator(@Nonnull UmbrellaAnimator umbrellaAnimator) {
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SLEEP, constantAnimation("sleep", 12.0F, 0)));
        return new VillagerAnimator(resolver, LayerStackAnimators.builder()
                .umbrella(umbrellaAnimator)
                .build(), FILLER_ENTITY_ID);
    }

    private static VillagerAnimator animator(@Nonnull AnimationResolver resolver) {
        return new VillagerAnimator(resolver, LayerStackAnimators.builder().build(), FILLER_ENTITY_ID);
    }

    private static AnimationFrame sample(@Nonnull VillagerAnimator animator, long gameTime, float partialTicks) {
        return animator.sample(gameTime, partialTicks, LocomotionAnimationContext.idle(), false);
    }

    private static ArmConfiguration armConfiguration(@Nonnull VillagerAnimator animator,
                                                     long gameTime,
                                                     float partialTicks) {
        return animator.currentArmConfiguration(gameTime, partialTicks, LocomotionAnimationContext.idle());
    }

    private static AnimationResolver resolver(Map<AnimationArchetype, KeyframeAnimation> animations) {
        return (archetype, context) -> animations.get(archetype);
    }

    private static KeyframeAnimation constantAnimation(String name, float value, int blendInTicks) {
        return animation(name, blendInTicks, List.of(AnimationTrack.<Float>builder()
                .target(AnimationTestTargets.FLOAT)
                .keyframes(List.of(new Keyframe<>(0, value, Easing.LINEAR)))
                .build()));
    }

    private static KeyframeAnimation animation(String name, int blendInTicks, List<AnimationTrack<?>> tracks) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(blendInTicks)
                .blendOutTicks(0)
                .tracks(tracks)
                .build();
    }

    private static KeyframeAnimation animation(String name,
                                               int blendInTicks,
                                               int durationTicks,
                                               int blendOutTicks,
                                               float value,
                                               ArmConfiguration armConfiguration) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(blendInTicks)
                .blendOutTicks(blendOutTicks)
                .tracks(List.of(AnimationTrack.<Float>builder()
                        .target(AnimationTestTargets.FLOAT)
                        .keyframes(List.of(new Keyframe<>(0, value, Easing.LINEAR)))
                        .build()))
                .armConfiguration(armConfiguration)
                .build();
    }

    /**
     * Stands in for the real state machine's contract without its clips: visible exactly while the last
     * gate it was advanced with was open.
     */
    private static final class GatedUmbrellaAnimator implements UmbrellaAnimator {

        private boolean deployed;

        @Override
        public void advance(@Nonnull UmbrellaAnimationContext context) {
            this.deployed = context.shouldDeploy();
        }

        @Override
        public AnimationFrame sample(@Nonnull UmbrellaAnimationContext context) {
            return AnimationFrame.of(Map.of(AnimationTestTargets.ABSOLUTE_FLOAT, UMBRELLA_VALUE));
        }

        @Override
        public boolean isVisible(@Nonnull UmbrellaAnimationContext context) {
            return this.deployed;
        }

    }

    private static final class AlwaysVisibleUmbrellaAnimator implements UmbrellaAnimator {

        @Override
        public void advance(@Nonnull UmbrellaAnimationContext context) {
        }

        @Override
        public AnimationFrame sample(@Nonnull UmbrellaAnimationContext context) {
            return AnimationFrame.of(Map.of(AnimationTestTargets.ABSOLUTE_FLOAT, UMBRELLA_VALUE));
        }

        @Override
        public boolean isVisible(@Nonnull UmbrellaAnimationContext context) {
            return true;
        }

    }

    private static KeyframeAnimation timelineArmAnimation(String name) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(30)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(List.of())
                .armConfigurationKeyframes(List.of(
                        new ArmConfigurationKeyframe(0, ArmConfiguration.BOTH_STRAIGHT),
                        new ArmConfigurationKeyframe(20, ArmConfiguration.BOTH_CROSSED)))
                .build();
    }

}
