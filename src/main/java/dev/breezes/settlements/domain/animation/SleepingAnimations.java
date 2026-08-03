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
 * Looping sleep pose: folded arms rising and falling with breath, eyes held shut, and an
 * occasional snore expressed via mouth open plus squash/stretch and a wobbling nose.
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
                // Arms rise and fall with each breath, peaking mid-cycle and returning to rest at the loop end.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(48, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(92, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(48, new Vec3(0.0, -0.25, -0.05), Easing.CUBIC),
                                new Keyframe<>(92, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Eyelids are held fully closed for the entire loop.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                // Head dips forward at peak inhale, returns to neutral before the arm breath cycle closes.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(70, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Mouth opens for the snore peak then drops back closed well before the loop end.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MOUTH_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(54, new Vec3(0.0, 0.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(80, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Squash/stretch sells the snore vibration: the mouth widens as air builds then bounces
                // back at release.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.MOUTH_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(0.85f, 1.15f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(40, new Vector3f(0.65f, 1.35f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(54, new Vector3f(0.5f, 1.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(76, new Vector3f(0.95f, 1.05f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(80, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                // Nose tilts down through a double-dip snore vibration before retracting to neutral.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-1.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(48, RotationUtil.degrees(-11.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(52, RotationUtil.degrees(-9.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(56, RotationUtil.degrees(-11.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(60, RotationUtil.degrees(-9.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.02, -0.05), Easing.CUBIC),
                                new Keyframe<>(32, new Vec3(0.0, 0.05, -0.1), Easing.CUBIC),
                                new Keyframe<>(56, new Vec3(0.0, -0.02, -0.05), Easing.CUBIC),
                                new Keyframe<>(80, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Torso breathes in sync with the arms: forward tilt at the same inhale peak.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(48, RotationUtil.degrees(1.2f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(92, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(48, new Vec3(0.0, -0.12, 0.05), Easing.CUBIC),
                                new Keyframe<>(92, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Legs are splayed at rest for the whole loop.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 2.5f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, -2.5f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
