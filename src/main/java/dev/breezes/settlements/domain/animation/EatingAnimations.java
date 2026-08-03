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
public final class EatingAnimations {

    // One-shot chew cycle: arms rise to eating position, head bobs to simulate chewing,
    // mouth and nose animate in sympathy, then everything settles back to neutral at tick 40.
    public static final int EAT_DURATION_TICKS = 40;

    public static KeyframeAnimation eat() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/eating/eat"))
                .durationTicks(EAT_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms rise to eating height and hold, returning to rest at the final tick.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(16, RotationUtil.degrees(-33.0f, 1.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(-37.0f, -1.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(32, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Arms drift forward and down to the mouth, matching the rotation arc.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(8, new Vec3(0.0, -0.5, -1.0), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(0.0, 0.0, -1.1), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, -0.5, -0.9), Easing.CUBIC),
                                new Keyframe<>(32, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(40, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Head tilts down to meet the food then returns upright at the end.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(11.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(27, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(32, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // The chew bob: head sinks toward the food on each bite and rises back between bites.
                // This is the only clip in the import driving HEAD_TRANSLATION.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.HEAD_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(8, new Vec3(0.0, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.2, 0.0), Easing.CUBIC),
                                new Keyframe<>(13, new Vec3(0.0, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(17, new Vec3(0.0, 0.25, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, 0.2, 0.0), Easing.CUBIC),
                                new Keyframe<>(27, new Vec3(0.0, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(32, new Vec3(0.0, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(40, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Mouth opens on each bite, starting at tick 8 (arms already raised) rather than tick 0.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MOUTH_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(10, new Vec3(0.0, 0.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(13, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(17, new Vec3(0.0, 0.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, 0.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(27, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Mouth widens in sync with each open, same cadence as the translation above.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.MOUTH_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(8, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(10, new Vector3f(1.0f, 1.4f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(13, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(17, new Vector3f(1.0f, 1.4f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(24, new Vector3f(1.0f, 1.4f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(27, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                // Nose secondary motion: slight dip on each chew to add face liveliness.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(22, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(26, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(29, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, -0.15, -0.05), Easing.CUBIC),
                                new Keyframe<>(15, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(19, new Vec3(0.0, -0.15, -0.05), Easing.CUBIC),
                                new Keyframe<>(22, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(26, new Vec3(0.0, -0.15, -0.05), Easing.CUBIC),
                                new Keyframe<>(29, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Torso holds a steady forward lean over the food, deepening slightly at the midpoint.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(2.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(2.5f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Static poses held for the whole clip: eyes cast down toward the food and legs splayed.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.8f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.1, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.8f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
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
