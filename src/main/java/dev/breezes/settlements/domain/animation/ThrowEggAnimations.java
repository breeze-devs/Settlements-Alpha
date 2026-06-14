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
public final class ThrowEggAnimations {

    // The clip is exactly 0.25s = 5 ticks; the behavior reuses it across an egg burst
    // by keeping loopMode LOOP, so this constant is also the cycle period.
    public static final int THROW_DURATION_TICKS = 5;

    public static KeyframeAnimation throwEgg() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/throw/throw_egg"))
                .durationTicks(THROW_DURATION_TICKS)
                // LOOP because the behavior sustains this across a multi-egg burst;
                // the behavior sets IDLE when the burst ends rather than letting it run ONCE.
                .loopMode(LoopMode.LOOP)
                .blendInTicks(1)
                .blendOutTicks(1)
                // arm_straight_left and arm_straight_right are the driven bones,
                // so the straight-arm geometry must be visible for the windmill to show.
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                // arm_straight_left: full 360° windmill offset so left arm leads right by 180°.
                // The 540° end value is intentional — one continuous forward-spin from 180° to 540°.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(180.0f, 0.0f, -10.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(540.0f, 0.0f, -10.0f), Easing.LINEAR)
                        ))
                        .build())
                // arm_straight_right: full 360° windmill from rest, completing one revolution per cycle.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 10.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(360.0f, 0.0f, 10.0f), Easing.LINEAR)
                        ))
                        .build())
                // torso: a constant 2° forward lean authored as two keyframes so the track is
                // well-formed — a single keyframe would be uninterpolated and may be dropped.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // torso position: a double-bob per revolution synced to the arm cycle.
                // Source posVec uses vanilla's internal -Y convention; we negate Y on import
                // so posVec(0,-0.08,0) → new Vec3(0.0, 0.08, 0.0) (body dips down visually).
                // Tick mapping (0.0625*20=1.25→1, 0.125*20=2.5→3 half-up, 0.1875*20=3.75→4, 0.25*20=5).
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(1, new Vec3(0.0, 0.08, 0.0), Easing.LINEAR),
                                new Keyframe<>(3, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 0.08, 0.0), Easing.LINEAR),
                                new Keyframe<>(5, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
