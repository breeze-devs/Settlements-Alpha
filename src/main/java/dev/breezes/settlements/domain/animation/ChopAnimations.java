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
public final class ChopAnimations {

    public static final int CHOP_DURATION_TICKS = 30;
    public static final int CHOP_IMPACT_TICKS = 14;

    public static KeyframeAnimation chopCrossedArms() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/swing/chop_crossed_arms"))
                .durationTicks(CHOP_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(4, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(-50.0f, 5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(30.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(30.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(4, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(0.0, -1.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(15, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(21, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(-7.5f, -10.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(29, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(0.0, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(15, new Vec3(0.0, 0.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(19, new Vec3(0.0, 0.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(29, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(7.5f, 20.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-2.5f, -10.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(0.0f, -10.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(26, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, -2.0, -1.87), Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, 0.0, -4.0), Easing.CUBIC),
                                new Keyframe<>(18, new Vec3(0.0, 0.0, -4.0), Easing.CUBIC),
                                new Keyframe<>(26, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(10.0f, 0.0f, 10.0f), Easing.CUBIC),
                                new Keyframe<>(16, RotationUtil.degrees(-50.0f, 0.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(0.0f, 5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(25, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-15.0f, 5.0f, -3.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(25.0f, -6.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(25.0f, -6.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.0, -0.5), Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, 2.0, 0.5), Easing.CUBIC),
                                new Keyframe<>(18, new Vec3(0.0, 1.8, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, 0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(-5.0f, 5.0f, 2.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(20.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(15.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, -0.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(15, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.2f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.8f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.2f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.8f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
