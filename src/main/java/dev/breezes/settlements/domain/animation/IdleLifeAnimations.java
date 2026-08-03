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
public final class IdleLifeAnimations {

    // The final hold creates a natural pause between breathing cycles
    public static final int BASE_IDLE_DURATION_TICKS = 80;
    public static final int BLINK_DURATION_TICKS = 6;
    public static final int PONDER_DURATION_TICKS = 80;

    /**
     * Looping breathe animation for the at-rest state.
     * <p>
     * A hold keyframe is appended at tick 80 (= the tick-60 neutral) on every track so the loop
     * pauses for one second between breathe cycles rather than snapping straight back to the top.
     */
    public static KeyframeAnimation baseIdle() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/base_idle"))
                .durationTicks(BASE_IDLE_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-1.11f, 0.0f, 0.24f), Easing.LINEAR),
                                new Keyframe<>(2, RotationUtil.degrees(-0.1f, 0.0f, 0.32f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(0.75f, 0.0f, 0.4f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(1.42f, 0.0f, 0.45f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(1.85f, 0.0f, 0.49f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(2.0f, 0.0f, 0.5f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(1.85f, 0.0f, 0.49f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(1.42f, 0.0f, 0.45f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(0.75f, 0.0f, 0.4f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-0.1f, 0.0f, 0.32f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(-2.22f, 0.0f, 0.15f), Easing.LINEAR),
                                new Keyframe<>(28, RotationUtil.degrees(-5.78f, 0.0f, -0.15f), Easing.LINEAR),
                                new Keyframe<>(32, RotationUtil.degrees(-7.9f, 0.0f, -0.32f), Easing.LINEAR),
                                new Keyframe<>(34, RotationUtil.degrees(-8.75f, 0.0f, -0.4f), Easing.LINEAR),
                                new Keyframe<>(36, RotationUtil.degrees(-9.42f, 0.0f, -0.45f), Easing.LINEAR),
                                new Keyframe<>(38, RotationUtil.degrees(-9.85f, 0.0f, -0.49f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-10.0f, 0.0f, -0.5f), Easing.LINEAR),
                                new Keyframe<>(42, RotationUtil.degrees(-9.85f, 0.0f, -0.49f), Easing.LINEAR),
                                new Keyframe<>(44, RotationUtil.degrees(-9.42f, 0.0f, -0.45f), Easing.LINEAR),
                                new Keyframe<>(46, RotationUtil.degrees(-8.75f, 0.0f, -0.4f), Easing.LINEAR),
                                new Keyframe<>(48, RotationUtil.degrees(-7.9f, 0.0f, -0.32f), Easing.LINEAR),
                                new Keyframe<>(52, RotationUtil.degrees(-5.78f, 0.0f, -0.15f), Easing.LINEAR),
                                new Keyframe<>(58, RotationUtil.degrees(-2.22f, 0.0f, 0.15f), Easing.LINEAR),
                                new Keyframe<>(60, RotationUtil.degrees(-1.11f, 0.0f, 0.24f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(-1.11f, 0.0f, 0.24f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.5, -0.5), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(60, new Vec3(0.0, 0.5, -0.5), Easing.CUBIC),
                                new Keyframe<>(80, new Vec3(0.0, 0.5, -0.5), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(3.38f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(1.94f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(0.78f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.36f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(0.1f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(0.1f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(0.36f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(0.78f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(1.94f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(34, RotationUtil.degrees(5.56f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(38, RotationUtil.degrees(6.72f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(7.14f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(42, RotationUtil.degrees(7.4f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(44, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(46, RotationUtil.degrees(7.4f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(48, RotationUtil.degrees(7.14f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(50, RotationUtil.degrees(6.72f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(54, RotationUtil.degrees(5.56f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(60, RotationUtil.degrees(3.38f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(3.38f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(60, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(80, new Vec3(0.1, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(24, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(60, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(80, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(-0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(60, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(80, new Vec3(-0.1, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(24, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(60, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(80, new Vector3f(1.0f, 1.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(-1.35f, -2.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(46, RotationUtil.degrees(-1.35f, 2.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(60, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, -1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 10.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Single-shot eyelid blink that closes both eyelids before returning to neutral.
     */
    public static KeyframeAnimation blink() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/blink"))
                .durationTicks(BLINK_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * The villager furrows its brow, casts its eyes down and to one side, and shifts its weight
     * in a small double-bob as if turning a thought over.
     */
    public static KeyframeAnimation ponder() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/ponder"))
                .durationTicks(PONDER_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(8)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-2.11f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-1.17f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-0.05f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-0.05f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-1.17f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(32, RotationUtil.degrees(-3.19f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-5.39f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(44, RotationUtil.degrees(-6.33f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(48, RotationUtil.degrees(-7.04f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(52, RotationUtil.degrees(-7.45f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(56, RotationUtil.degrees(-7.45f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(60, RotationUtil.degrees(-7.04f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(64, RotationUtil.degrees(-6.33f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(72, RotationUtil.degrees(-4.31f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(-2.11f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.5, -0.5), Easing.CUBIC),
                                new Keyframe<>(40, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(80, new Vec3(0.0, 0.5, -0.5), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-10.0f, -18.0f, -4.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(-10.0f, -18.0f, -4.0f), Easing.CUBIC),
                                new Keyframe<>(50, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(56, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(62, RotationUtil.degrees(-2.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(68, RotationUtil.degrees(6.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, -0.15, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(0.0, -0.15, 0.0), Easing.LINEAR),
                                new Keyframe<>(50, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(40, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(48, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(-0.3, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(40, new Vec3(-0.3, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(48, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-1.35f, -2.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(60, RotationUtil.degrees(-1.35f, 2.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(5.0f, 0.0f, 5.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(5.0f, 0.0f, 5.0f), Easing.CUBIC),
                                new Keyframe<>(54, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, -1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 10.0f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
