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
public final class PickUpAnimations {

    public static final int PICK_UP_DURATION_TICKS = 24;
    public static final int PICK_UP_AT_TICK = 11;

    public static KeyframeAnimation pickUp() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/gesture/pick_up"))
                .durationTicks(PICK_UP_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(5.0f, -2.0f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(5.0f, -10.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(5.0f, -10.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(8.0f, -2.0f, -1.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(0.0, 1.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, 1.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(24, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(4, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(9, RotationUtil.degrees(25.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(25.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(9, new Vec3(0.0, 1.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(17, new Vec3(0.0, 0.5, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(4, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(9, new Vec3(0.0, 0.0, -4.0), Easing.CUBIC),
                                new Keyframe<>(24, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(3, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(-50.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-40.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(4, RotationUtil.degrees(12.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(12.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(4, new Vec3(0.0, 1.0, 0.2), Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 4.5, 1.0), Easing.CUBIC),
                                new Keyframe<>(13, new Vec3(0.0, 4.5, 1.0), Easing.CUBIC),
                                new Keyframe<>(18, new Vec3(0.0, 1.5, 0.3), Easing.CUBIC),
                                new Keyframe<>(24, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
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
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.MOUTH_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(4, RotationUtil.degrees(0.0f, 0.0f, 7.5f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(0.0f, 0.0f, 7.5f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .build();
    }

}
