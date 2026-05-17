package dev.breezes.settlements.domain.animation;

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
                .loopMode(LoopMode.HOLD_LAST)
                .blendInTicks(blendInTicks)
                .blendOutTicks(0)
                .tracks(tracks)
                .build();
    }

}
