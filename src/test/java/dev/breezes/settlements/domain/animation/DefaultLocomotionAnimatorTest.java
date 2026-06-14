package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultLocomotionAnimatorTest {

    private static final float VANILLA_LIMB_SWING_CYCLE = (float) ((Math.PI * 2.0D) / 0.6662D);

    @Test
    void sample_whenStandingStill_returnsEmptyFrame() {
        // Arrange
        DefaultLocomotionAnimator animator = new DefaultLocomotionAnimator(library(Map.of(
                NavigationType.WALK, constantAnimation("walk", 10.0F))));
        LocomotionAnimationContext context = new LocomotionAnimationContext(NavigationType.WALK, 4.0F, 0.0F);

        // Act
        AnimationFrame frame = animator.sample(context);

        // Assert
        assertEquals(0.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_usesNavigationTypeToSelectClip() {
        // Arrange
        DefaultLocomotionAnimator animator = new DefaultLocomotionAnimator(library(Map.of(
                NavigationType.WALK, constantAnimation("walk", 10.0F),
                NavigationType.SPRINT, constantAnimation("sprint", 20.0F))));
        LocomotionAnimationContext context = new LocomotionAnimationContext(NavigationType.SPRINT, 0.0F, 1.0F);

        // Act
        AnimationFrame frame = animator.sample(context);

        // Assert
        assertEquals(20.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_usesLimbSwingAsAnimationPhase() {
        // Arrange
        DefaultLocomotionAnimator animator = new DefaultLocomotionAnimator(library(Map.of(
                NavigationType.WALK, phasedAnimation("walk"))));
        LocomotionAnimationContext quarterCycle = new LocomotionAnimationContext(
                NavigationType.WALK,
                VANILLA_LIMB_SWING_CYCLE / 4.0F,
                1.0F);

        // Act
        AnimationFrame frame = animator.sample(quarterCycle);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void activeArmConfiguration_whenMoving_returnsClipArmConfiguration() {
        // Arrange
        DefaultLocomotionAnimator animator = new DefaultLocomotionAnimator(library(Map.of(
                NavigationType.WALK, constantAnimation("walk", 10.0F))));
        LocomotionAnimationContext context = new LocomotionAnimationContext(NavigationType.WALK, 0.0F, 1.0F);

        // Act
        Optional<ArmConfiguration> configuration = animator.activeArmConfiguration(context);

        // Assert
        assertEquals(Optional.of(ArmConfiguration.BOTH_CROSSED), configuration);
    }

    private static LocomotionAnimationLibrary library(Map<NavigationType, KeyframeAnimation> animations) {
        return navigationType -> animations.get(navigationType);
    }

    private static KeyframeAnimation constantAnimation(String name, float value) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/locomotion/" + name))
                .durationTicks(20)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(java.util.List.of(AnimationTrack.<Float>builder()
                        .target(AnimationTestTargets.FLOAT)
                        .keyframes(java.util.List.of(new Keyframe<>(0, value, Easing.LINEAR)))
                        .build()))
                .armConfiguration(ArmConfiguration.BOTH_CROSSED)
                .build();
    }

    private static KeyframeAnimation phasedAnimation(String name) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/locomotion/" + name))
                .durationTicks(20)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(java.util.List.of(AnimationTrack.<Float>builder()
                        .target(AnimationTestTargets.FLOAT)
                        .keyframes(java.util.List.of(
                                new Keyframe<>(0, 0.0F, Easing.LINEAR),
                                new Keyframe<>(5, 5.0F, Easing.LINEAR),
                                new Keyframe<>(10, 10.0F, Easing.LINEAR)))
                        .build()))
                .armConfiguration(ArmConfiguration.BOTH_CROSSED)
                .build();
    }

}
