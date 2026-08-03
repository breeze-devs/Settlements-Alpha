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
public final class WaveAnimations {

    private static final int DURATION_TICKS = 30;

    public static KeyframeAnimation wave() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/gesture/wave"))
                .durationTicks(DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.LEFT_CROSSED_RIGHT_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-127.5f, 80.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(9, RotationUtil.degrees(-112.5f, 80.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(-127.5f, 80.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-115.0f, 80.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-127.5f, 80.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(28, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(5.0f, 2.0f, -1.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(5.0f, 4.0f, -2.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(5.0f, -2.0f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(5.0f, 4.0f, -2.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(5.0f, -2.0f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(5.0f, 2.0f, -1.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(7, RotationUtil.degrees(5.0f, -7.5f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(5.0f, -7.5f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, -7.5f, -1.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(7, RotationUtil.degrees(-5.0f, 7.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(-5.0f, 7.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 7.5f, 1.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-1.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(9, new Vec3(0.0, 0.05, 0.0), Easing.LINEAR),
                                new Keyframe<>(14, new Vec3(0.0, -0.05, 0.0), Easing.LINEAR),
                                new Keyframe<>(19, new Vec3(0.0, 0.03, 0.0), Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, -0.02, 0.0), Easing.LINEAR),
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(6, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(5.0f, 0.0f, 2.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(5.0f, 0.0f, 2.0f), Easing.CUBIC),
                                new Keyframe<>(21, RotationUtil.degrees(4.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(0.0f, 0.0f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(0.0f, 0.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(21, RotationUtil.degrees(0.0f, 0.0f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(6, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(6, new Vec3(0.0, -0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, -0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(28, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(6, new Vector3f(1.0f, 1.4f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(20, new Vector3f(1.0f, 1.4f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(27, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(6, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(27, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(6, new Vec3(0.0, -0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, -0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(28, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(6, new Vector3f(1.0f, 1.4f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(20, new Vector3f(1.0f, 1.4f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(27, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(6, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(27, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)))
                        .build())
                .build();
    }
}
