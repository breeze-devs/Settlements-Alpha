package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * TODO: these are not real authored animations, these are stubs
 *       Delete or replace these once we author real ones
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdleLifeAnimations {

    public static KeyframeAnimation baseIdle() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/base_idle"))
                .durationTicks(80)
                .loopMode(LoopMode.LOOP)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, -0.15, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(40, Vec3.ZERO, Easing.EASE_IN_OUT),
                                new Keyframe<>(60, new Vec3(0.0, 0.1, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(80, Vec3.ZERO, Easing.EASE_IN_OUT)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(1.5F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(80, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT)))
                        .build())
                .build();
    }

    public static KeyframeAnimation blink() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/blink"))
                .durationTicks(6)
                .loopMode(LoopMode.ONCE)
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 2.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(4, new Vec3(0.0, 2.0, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.EASE_IN_OUT)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 2.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(4, new Vec3(0.0, 2.0, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.EASE_IN_OUT)))
                        .build())
                .build();
    }

    public static KeyframeAnimation glanceAround() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/glance_around"))
                .durationTicks(70)
                .loopMode(LoopMode.ONCE)
                .blendOutTicks(8)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(0.0F, 4.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(42, RotationUtil.degrees(0.0F, -3.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(70, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(18, new Vec3(0.25, 0.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(42, new Vec3(-0.2, 0.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(70, Vec3.ZERO, Easing.EASE_IN_OUT)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(18, new Vec3(0.25, 0.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(42, new Vec3(-0.2, 0.0, 0.0), Easing.EASE_IN_OUT),
                                new Keyframe<>(70, Vec3.ZERO, Easing.EASE_IN_OUT)))
                        .build())
                .build();
    }

    public static KeyframeAnimation adjustSleeves() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/adjust_sleeves"))
                .durationTicks(60)
                .loopMode(LoopMode.ONCE)
                .blendOutTicks(6)
                .arms(ArmConfiguration.LEFT_CROSSED_RIGHT_STRAIGHT)
                .armConfigurationAt(42, ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-45.0F, 0.0F, -12.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(28, RotationUtil.degrees(-50.0F, 0.0F, 8.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(42, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(60, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(1.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(42, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(60, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR)))
                        .build())
                .build();
    }

}
