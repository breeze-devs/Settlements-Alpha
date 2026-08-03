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
public final class DigAnimations {

    public static final int DIG_DURATION_TICKS = 30;
    public static final int DIG_IMPACT_TICKS = 17;

    public static KeyframeAnimation dig() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/mason/dig"))
                .durationTicks(DIG_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-38.9494f, 15.7932f, 20.9288f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-88.9494f, 15.7932f, 20.9288f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, 1.0, -4.0), Easing.CUBIC),
                                new Keyframe<>(17, new Vec3(0.0, 0.0, -3.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-55.9673f, -13.4856f, -8.3635f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-100.9673f, -13.4856f, -8.3635f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(-0.5, 1.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(17, new Vec3(0.0, 0.0, 3.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(25.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(10.55f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(10.55f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-2.5f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-2.5f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(15.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(5.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.0, 3.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-32.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(32.0f, 6.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(35.0f, 5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(35.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(8, new Vec3(0.0, 4.5, 2.5), Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, 5.0, 2.7), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 4.5, 2.5), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
