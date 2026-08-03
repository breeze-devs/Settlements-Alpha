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
 * A fast, continuous overhead windmill.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThrowEggAnimations {

    public static final int THROW_DURATION_TICKS = 5;

    public static KeyframeAnimation throwEgg() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/throw/throw_egg"))
                .durationTicks(THROW_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(4)
                .blendOutTicks(4)
                // arm_straight_left and arm_straight_right are the driven bones, so the straight-arm
                // geometry must be visible for the windmill to show.
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(180.0f, 0.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(540.0f, 0.0f, -10.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 10.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(360.0f, 0.0f, 10.0f), Easing.CUBIC)
                        ))
                        .build())
                // Nose flicks in reaction to the wind-up.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(2.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(2.5f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Torso rocks side to side (roll) in counterpoint to the windmill, holding a constant
                // forward lean (pitch).
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-3.0f, 0.0f, -3.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(-3.0f, 0.0f, 3.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-3.0f, 0.0f, -3.0f), Easing.CUBIC)
                        ))
                        .build())
                // Torso bobs twice per cycle, once per arm passing overhead.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(1, new Vec3(0.0, 0.08, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(4, new Vec3(0.0, 0.08, 0.0), Easing.CUBIC),
                                new Keyframe<>(5, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // A wide-eyed, startled face held for the whole clip: raised brow, wide eyes, dilated
                // pupils, and an open mouth.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 2.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.9, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.5f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 2.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.9, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.5f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MOUTH_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.MOUTH_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.8f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                // Legs splay outward into a braced stance held for the whole clip.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
