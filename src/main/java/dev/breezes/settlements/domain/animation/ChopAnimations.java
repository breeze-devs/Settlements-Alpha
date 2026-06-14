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
                .blendInTicks(1)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-70.0f, 5.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(30.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(25.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(27, RotationUtil.degrees(-1.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(3, new Vec3(0.0, 0.05, 0.05), Easing.LINEAR),
                                new Keyframe<>(13, new Vec3(0.0, -0.1, -0.1), Easing.LINEAR),
                                new Keyframe<>(17, new Vec3(0.0, 0.15, 0.15), Easing.LINEAR),
                                new Keyframe<>(21, new Vec3(0.0, 0.05, 0.05), Easing.LINEAR),
                                new Keyframe<>(27, new Vec3(0.0, 0.0, -0.02), Easing.LINEAR),
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(25.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(10, new Vec3(0.0, 0.0, -1.5), Easing.LINEAR),
                                new Keyframe<>(14, new Vec3(0.0, 1.0, 2.0), Easing.LINEAR),
                                new Keyframe<>(18, new Vec3(0.0, 0.8, 1.5), Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, 0.3, 0.5), Easing.LINEAR),
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
