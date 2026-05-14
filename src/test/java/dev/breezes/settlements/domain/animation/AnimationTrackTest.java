package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimationTrackTest {

    @Test
    void sample_returnsExactKeyframeValuesAtKeyframeTicks() {
        // Arrange
        AnimationTrack<Float> track = track(
                new Keyframe<>(0, 0.0F, Easing.LINEAR),
                new Keyframe<>(10, 10.0F, Easing.LINEAR));

        // Act, Assert
        assertEquals(0.0F, track.sample(0.0F), 0.0001F);
        assertEquals(10.0F, track.sample(10.0F), 0.0001F);
    }

    @Test
    void sample_interpolatesBetweenKeyframesUsingCurrentKeyframeEasing() {
        // Arrange
        AnimationTrack<Float> track = track(
                new Keyframe<>(0, 0.0F, Easing.EASE_IN),
                new Keyframe<>(10, 10.0F, Easing.LINEAR));

        // Act
        float result = track.sample(5.0F);

        // Assert
        assertEquals(2.5F, result, 0.0001F);
    }

    @Test
    void constructor_sortsKeyframesByTick() {
        // Arrange
        AnimationTrack<Float> track = track(
                new Keyframe<>(10, 10.0F, Easing.LINEAR),
                new Keyframe<>(0, 0.0F, Easing.LINEAR));

        // Act, Assert
        assertEquals(5.0F, track.sample(5.0F), 0.0001F);
    }

    @Test
    void constructor_rejectsDuplicateTicks() {
        // Arrange, Act, Assert
        assertThrows(IllegalArgumentException.class, () -> track(
                new Keyframe<>(0, 0.0F, Easing.LINEAR),
                new Keyframe<>(0, 10.0F, Easing.LINEAR)));
    }

    @SafeVarargs
    private static AnimationTrack<Float> track(Keyframe<Float>... keyframes) {
        return AnimationTrack.<Float>builder()
                .target(AnimationTestTargets.FLOAT)
                .keyframes(List.of(keyframes))
                .build();
    }
}
