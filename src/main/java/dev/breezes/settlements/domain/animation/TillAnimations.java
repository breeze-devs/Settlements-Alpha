package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TillAnimations {

    public static final int TILL_DURATION_TICKS = 25;
    public static final int ARM_RETURN_TO_CROSSED_AT_TICK = 20;
    public static final int TILL_IMPACT_TICK = 10;

    public static KeyframeAnimation till() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/farming/till"))
                .durationTicks(TILL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .armConfigurationAt(ARM_RETURN_TO_CROSSED_AT_TICK, ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-70.0f, 20.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-77.5f, 20.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-24.1436f, 26.2132f, 8.5267f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-31.6436f, 26.2132f, 8.5267f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(5, new Vec3(0.25, 0.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(23, Vec3.ZERO, Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-71.0546f, -29.5016f, -3.4396f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-78.5546f, -29.5016f, -3.4396f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-13.5335f, -35.7485f, -12.7245f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-26.0335f, -35.7485f, -12.7245f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(5, new Vec3(-0.25, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(23, Vec3.ZERO, Easing.CUBIC)))
                        .build())
                // The asymmetric yaw keeps the stance planted throughout the swing.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(5.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(5.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-25.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(32.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(16, RotationUtil.degrees(32.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(25, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(5, new Vec3(0.0, 1.5, 0.0), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(0.0, 1.5, 0.0), Easing.CUBIC),
                                new Keyframe<>(25, Vec3.ZERO, Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(22.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(30.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(22.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.LINEAR)))
                        .build())
                .build();
    }

}
