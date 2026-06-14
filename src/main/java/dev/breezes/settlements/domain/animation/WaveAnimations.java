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
                .blendInTicks(2)
                .blendOutTicks(3)
                .arms(ArmConfiguration.LEFT_CROSSED_RIGHT_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-162.5f, 0f, -20f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(-162.5f, 0f, -15f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-162.5f, 0f, -45f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-162.5f, 0f, -15f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(-162.5f, 0f, -45f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(-162.5f, 0f, -20f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(5f, 2f, -1f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(5f, 4f, -2f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(5f, -2f, 1f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(5f, 4f, -2f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(5f, -2f, 1f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(5f, 2f, -1f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(-3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-2f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(2f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-1f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(9, new Vec3(0f, 0.05f, 0f), Easing.LINEAR),
                                new Keyframe<>(14, new Vec3(0f, -0.05f, 0f), Easing.LINEAR),
                                new Keyframe<>(19, new Vec3(0f, 0.03f, 0f), Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0f, -0.02f, 0f), Easing.LINEAR),
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(3f, 0f, -1f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(3f, 0f, -2f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(3f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(3f, 0f, -2f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(3f, 0f, -1f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .build();
    }
}
