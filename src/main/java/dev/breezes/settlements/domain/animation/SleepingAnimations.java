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
 * Looping sleep pose: folded arms rising and falling with breath, eyes held shut,
 * and an occasional snore expressed via mouth open + squash/stretch on the mouth scale.
 * The source clip's arm_straight channels were intentionally dropped because the sleep
 * pose keeps arms crossed, so the straight-arm geometry is hidden.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SleepingAnimations {

    // 92 ticks covers one full breath + snore cycle before seamlessly looping.
    public static final int SLEEP_DURATION_TICKS = 92;

    public static KeyframeAnimation sleeping() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/sleeping/sleep"))
                .durationTicks(SLEEP_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms rise and fall slightly with each breath cycle.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(2.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(70, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(0.0, -0.25, -0.05), Easing.LINEAR),
                                new Keyframe<>(70, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Eyelids are held at the fully-closed position for the entire loop;
                // two identical keyframes ensure the track is well-formed.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(92, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(92, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                // Head dips forward at peak inhale, returns to neutral at exhale.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(70, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Mouth opens for the snore peak then drops back closed.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MOUTH_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(54, new Vec3(0.0, 0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(76, new Vec3(0.0, 0.05, 0.0), Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Squash/stretch on the mouth geometry sells the snore vibration —
                // the mouth squashes wide as air builds up then bounces back at release.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.MOUTH_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0F, 1.0F, 1.0F), Easing.LINEAR),
                                new Keyframe<>(20, new Vector3f(0.85F, 1.15F, 1.0F), Easing.LINEAR),
                                new Keyframe<>(40, new Vector3f(0.65F, 1.35F, 1.0F), Easing.LINEAR),
                                new Keyframe<>(54, new Vector3f(0.5F, 1.5F, 1.0F), Easing.LINEAR),
                                new Keyframe<>(76, new Vector3f(0.95F, 1.05F, 1.0F), Easing.LINEAR),
                                new Keyframe<>(80, new Vector3f(1.0F, 1.0F, 1.0F), Easing.LINEAR)
                        ))
                        .build())
                // Nose tilts down and pushes forward during the snore build, retracts at release.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-1.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(56, RotationUtil.degrees(-6.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, 0.02, -0.05), Easing.LINEAR),
                                new Keyframe<>(32, new Vec3(0.0, 0.05, -0.1), Easing.LINEAR),
                                new Keyframe<>(56, new Vec3(0.0, -0.02, -0.05), Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Torso breathes in sync with the arms: forward tilt at inhale peak.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(1.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(70, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(0.0, -0.12, 0.05), Easing.LINEAR),
                                new Keyframe<>(70, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
