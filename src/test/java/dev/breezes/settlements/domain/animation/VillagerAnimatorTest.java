package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerAnimatorTest {

    @Test
    void onArchetypeChanged_startsNewAnimation() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, constantAnimation("swing", 12.0F, 0)));
        VillagerAnimator animator = new VillagerAnimator(resolver);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 20L);
        AnimationFrame frame = animator.sample(20L, 0.0F);

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
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 2, AnimationSelectionContext.generic(), 10L);
        AnimationFrame frame = animator.sample(12L, 0.0F);

        // Assert
        assertEquals(15.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_blendsMissingTargetsAgainstNeutralValue() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.INTERACT, constantAnimation("hold", 10.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 4, List.of())));
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 2, AnimationSelectionContext.generic(), 10L);
        AnimationFrame frame = animator.sample(12L, 0.0F);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_autoPopsTriggeredActionAfterDurationAndBlendOut() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.SWING_HEAVY, animation("swing", 0, 10, 2, 12.0F, null)));
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 20L);

        // Act
        AnimationFrame frameDuringBlendOut = animator.sample(31L, 0.0F);
        AnimationFrame frameAfterExpiry = animator.sample(33L, 0.1F);

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
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.EAT, (byte) 0, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 0L);
        AnimationFrame frame = animator.sample(20L, 0.0F);

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
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.SWING_HEAVY, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        animator.onMotionChanged(AnimationArchetype.EAT, (byte) 1, AnimationSelectionContext.generic(), 0L);
        AnimationFrame frame = animator.sample(50L, 0.0F);

        // Assert
        assertEquals(10.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void currentArmConfiguration_fallsBackToBaseWhenActionDoesNotOwnArmConfiguration() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.INTERACT, animation("interact", 0, 10, 0, 12.0F, null)));
        VillagerAnimator animator = new VillagerAnimator(resolver);

        // Act
        animator.onMotionChanged(AnimationArchetype.INTERACT, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Assert
        assertEquals(ArmConfiguration.BOTH_CROSSED, animator.currentArmConfiguration());
    }

    @Test
    void currentArmConfiguration_returnsActionOverrideUntilActionExpires() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.HARVEST, animation("harvest", 0, 10, 0, 12.0F, ArmConfiguration.BOTH_STRAIGHT)));
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.HARVEST, (byte) 1, AnimationSelectionContext.generic(), 0L);

        // Act
        ArmConfiguration activeConfig = animator.currentArmConfiguration();
        animator.sample(11L, 0.1F);

        // Assert
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, activeConfig);
        assertEquals(ArmConfiguration.BOTH_CROSSED, animator.currentArmConfiguration());
    }

    @Test
    void currentArmConfiguration_samplesActionTimeline() {
        // Arrange
        AnimationResolver resolver = resolver(Map.of(
                AnimationArchetype.IDLE, constantAnimation("idle", 0.0F, 0),
                AnimationArchetype.HARVEST, timelineArmAnimation("harvest")));
        VillagerAnimator animator = new VillagerAnimator(resolver);
        animator.onMotionChanged(AnimationArchetype.HARVEST, (byte) 1, AnimationSelectionContext.generic(), 10L);

        // Act, Assert
        assertEquals(ArmConfiguration.BOTH_STRAIGHT, animator.currentArmConfiguration(29L, 0.9F));
        assertEquals(ArmConfiguration.BOTH_CROSSED, animator.currentArmConfiguration(30L, 0.0F));
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
