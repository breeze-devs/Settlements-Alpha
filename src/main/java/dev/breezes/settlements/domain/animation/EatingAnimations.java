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
                .blendInTicks(2)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms rise to eating height and hold, returning to rest at the final tick.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-33.0f, 1.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-37.0f, -1.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(32, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Arms drift slightly forward and down to the mouth, matching the rotation arc.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(8, new Vec3(0.0, -1.5, -1.0), Easing.LINEAR),
                                new Keyframe<>(16, new Vec3(0.0, -1.6, -1.1), Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, -1.4, -0.9), Easing.LINEAR),
                                new Keyframe<>(32, new Vec3(0.0, -1.5, -1.0), Easing.LINEAR),
                                new Keyframe<>(40, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Head tilts down to meet the food then returns upright at the end.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(11.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(27, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(32, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Mouth opens on each bite, synced to the head bob cadence.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MOUTH_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(10, new Vec3(0.0, 0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(13, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(17, new Vec3(0.0, 0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(20, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, 0.4, 0.0), Easing.LINEAR),
                                new Keyframe<>(27, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Nose secondary motion: slight dip on each chew to add face liveliness.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(26, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(29, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(8, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(12, new Vec3(0.0, -0.15, -0.05), Easing.LINEAR),
                                new Keyframe<>(15, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(19, new Vec3(0.0, -0.15, -0.05), Easing.LINEAR),
                                new Keyframe<>(22, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(26, new Vec3(0.0, -0.15, -0.05), Easing.LINEAR),
                                new Keyframe<>(29, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
