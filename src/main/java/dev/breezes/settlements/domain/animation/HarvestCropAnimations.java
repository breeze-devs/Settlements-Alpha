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
public final class HarvestCropAnimations {

    public static final int HARVEST_DURATION_TICKS = 30;
    public static final int HARVEST_AT_TICK = 14;
    public static final int ARM_RETURN_TO_CROSSED_AT_TICK = 20;

    public static KeyframeAnimation harvestCrop() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/farming/harvest_crop"))
                .durationTicks(HARVEST_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .armConfigurationAt(ARM_RETURN_TO_CROSSED_AT_TICK, ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-50.0f, -10.0f, -15.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-55.0f, -8.0f, -17.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-50.0f, -10.0f, -15.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-80.0f, 10.0f, 10.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(-90.0f, -10.0f, 15.0f), Easing.CUBIC),
                                new Keyframe<>(16, RotationUtil.degrees(-70.0f, 20.0f, 5.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-80.0f, 10.0f, 10.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(8, new Vec3(0.0, 1.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(-0.5, 1.2, 1.2), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(0.5, 0.8, 0.8), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 1.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(-25.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(18, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 1.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(18, new Vec3(0.0, 1.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(-3.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-3.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(3.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(3.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(7, RotationUtil.degrees(-17.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(32.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(32.0f, 0.0f, 0.0f), Easing.CUBIC),
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
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
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
