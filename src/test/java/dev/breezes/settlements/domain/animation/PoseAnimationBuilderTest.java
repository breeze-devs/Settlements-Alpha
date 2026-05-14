package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PoseAnimationBuilderTest {

    @Test
    void build_generatesTracksFromPoseTimeline() {
        // Arrange
        KeyframeAnimation animation = KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/test/poses"))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .at(0, Pose.of(AnimationTestTargets.FLOAT, 0.0F), Easing.LINEAR)
                .at(10, Pose.of(AnimationTestTargets.FLOAT, 10.0F), Easing.LINEAR)
                .build();

        // Act
        AnimationFrame frame = animation.sample(5.0F);

        // Assert
        assertEquals(5.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void build_generatesUnionOfPoseTargets() {
        // Arrange
        KeyframeAnimation animation = KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/test/pose_union"))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .at(0, Pose.of(AnimationTestTargets.FLOAT, 1.0F)
                        .with(AnimationTestTargets.OTHER_FLOAT, 2.0F), Easing.LINEAR)
                .build();

        // Act
        AnimationFrame frame = animation.sample(0.0F);

        // Assert
        assertEquals(1.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(2.0F, frame.get(AnimationTestTargets.OTHER_FLOAT), 0.0001F);
    }

    @Test
    void build_manualTrackOverridesPoseTrackForSameTarget() {
        // Arrange
        AnimationTrack<Float> manualTrack = AnimationTrack.<Float>builder()
                .target(AnimationTestTargets.FLOAT)
                .keyframes(List.of(
                        new Keyframe<>(0, 100.0F, Easing.LINEAR),
                        new Keyframe<>(10, 200.0F, Easing.LINEAR)))
                .build();

        KeyframeAnimation animation = KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/test/manual_override"))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .at(0, Pose.of(AnimationTestTargets.FLOAT, 0.0F), Easing.LINEAR)
                .at(10, Pose.of(AnimationTestTargets.FLOAT, 10.0F), Easing.LINEAR)
                .track(manualTrack)
                .build();

        // Act
        AnimationFrame frame = animation.sample(5.0F);

        // Assert
        assertEquals(150.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void build_manualTrackCombinesWithDisjointPoseTargets() {
        // Arrange
        AnimationTrack<Float> manualTrack = AnimationTrack.<Float>builder()
                .target(AnimationTestTargets.OTHER_FLOAT)
                .keyframes(List.of(new Keyframe<>(0, 2.0F, Easing.LINEAR)))
                .build();

        KeyframeAnimation animation = KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/test/manual_union"))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .at(0, Pose.of(AnimationTestTargets.FLOAT, 1.0F), Easing.LINEAR)
                .track(manualTrack)
                .build();

        // Act
        AnimationFrame frame = animation.sample(0.0F);

        // Assert
        assertEquals(1.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(2.0F, frame.get(AnimationTestTargets.OTHER_FLOAT), 0.0001F);
    }

    @Test
    void build_sparsePoseTargetsInterpolateAcrossOmittedIntermediatePoses() {
        // Arrange
        KeyframeAnimation animation = KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/test/sparse_pose_channels"))
                .durationTicks(20)
                .loopMode(LoopMode.ONCE)
                .at(0, Pose.of(AnimationTestTargets.FLOAT, 0.0F), Easing.LINEAR)
                .at(10, Pose.of(AnimationTestTargets.OTHER_FLOAT, 100.0F), Easing.LINEAR)
                .at(20, Pose.of(AnimationTestTargets.FLOAT, 20.0F)
                        .with(AnimationTestTargets.OTHER_FLOAT, 200.0F), Easing.LINEAR)
                .build();

        // Act
        AnimationFrame frame = animation.sample(10.0F);

        // Assert
        assertEquals(10.0F, frame.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(100.0F, frame.get(AnimationTestTargets.OTHER_FLOAT), 0.0001F);
    }

    @Test
    void migratedButcheringSwing_samplesExpectedArmPose() {
        // Arrange
        KeyframeAnimation animation = ButcheringAnimations.swingHeavyAxe();

        // Act
        AnimationFrame frame = animation.sample(15.0F);

        // Assert
        assertEquals(Math.toRadians(-40.0F), frame.get(AnimationTargets.ARMS_ROTATION).x(), 0.0001F);
        assertFalse(frame.has(AnimationTestTargets.FLOAT));
    }

}
