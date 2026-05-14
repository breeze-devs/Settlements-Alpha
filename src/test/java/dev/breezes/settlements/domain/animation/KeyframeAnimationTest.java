package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class KeyframeAnimationTest {

    @Test
    void sample_returnsEmptyFrameWhenAnimationHasNoTracks() {
        // Arrange
        KeyframeAnimation animation = animation(LoopMode.ONCE, List.of());

        // Act
        AnimationFrame frame = animation.sample(5.0F);

        // Assert
        assertSame(AnimationFrame.EMPTY, frame);
    }

    @Test
    void sample_loopModeWrapsElapsedTicks() {
        // Arrange
        KeyframeAnimation animation = animation(LoopMode.LOOP, List.of(track()));

        // Act
        AnimationFrame frame = animation.sample(15.0F);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_holdLastClampsAtDuration() {
        // Arrange
        KeyframeAnimation animation = animation(LoopMode.HOLD_LAST, List.of(track()));

        // Act
        AnimationFrame frame = animation.sample(15.0F);

        // Assert
        assertEquals(10.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void sample_pingPongReversesAfterDuration() {
        // Arrange
        KeyframeAnimation animation = animation(LoopMode.PING_PONG, List.of(track()));

        // Act
        AnimationFrame frame = animation.sample(15.0F);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    private static KeyframeAnimation animation(LoopMode loopMode, List<AnimationTrack<?>> tracks) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test"))
                .durationTicks(10)
                .loopMode(loopMode)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(tracks)
                .build();
    }

    private static AnimationTrack<Float> track() {
        return AnimationTrack.<Float>builder()
                .target(AnimationTestTargets.FLOAT)
                .keyframes(List.of(
                        new Keyframe<>(0, 0.0F, Easing.LINEAR),
                        new Keyframe<>(10, 10.0F, Easing.LINEAR)))
                .build();
    }
}
