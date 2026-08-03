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
public final class FishingAnimations {

    public static final int CAST_DURATION_TICKS = 30;
    public static final int CAST_IMPACT_TICK = 17;

    public static final int FISHING_WAIT_DURATION_TICKS = 40;
    public static final int FIGHT_FISH_DURATION_TICKS = 24;
    public static final int REEL_DURATION_TICKS = 10;
    public static final int REEL_IMPACT_TICK = 3;

    private static final int BLEND_IN_TICKS = 4;
    private static final int BLEND_OUT_TICKS = 4;

    /**
     * One-shot over-the-shoulder rod cast. Arms swing back then release forward;
     * torso, legs and head all counter-rotate to sell the weight transfer.
     */
    public static KeyframeAnimation cast() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/cast_hook"))
                .durationTicks(CAST_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms wind back then snap through neutral to the forward release peak
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(-50.0f, 15.0f, 10.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(30.0f, -2.0f, -12.0f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Arms pull back on the body during wind-up, held through the release, then return
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(13, new Vec3(0.0, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Left leg dips back during wind-up, absorbs the forward transfer
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(10, RotationUtil.degrees(5.0f, -7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(0.0f, -7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(15, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.0, -1.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Right leg mirrors left for a balanced stance transfer
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(-2.5f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(15, RotationUtil.degrees(5.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(5.0f, 7.5f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 7.5f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(15, new Vec3(0.0, 0.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, 0.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Nose secondary wobble adds snappiness to the throw's reactive momentum
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-12.0f, 0.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(19, RotationUtil.degrees(-12.0f, 0.0f, -10.0f), Easing.CUBIC),
                                new Keyframe<>(23, RotationUtil.degrees(0.0f, 0.0f, 5.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Torso leans back on wind-up, thrusts forward on release, settles
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(-15.0f, 10.0f, 5.0f), Easing.CUBIC),
                                new Keyframe<>(16, RotationUtil.degrees(20.0f, -10.0f, -5.0f), Easing.CUBIC),
                                new Keyframe<>(22, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Torso translation sells the shift in center of mass.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, 0.0, -1.5), Easing.CUBIC),
                                new Keyframe<>(16, new Vec3(0.0, 1.0, 2.0), Easing.CUBIC),
                                new Keyframe<>(22, new Vec3(0.0, 0.3, 0.8), Easing.CUBIC),
                                new Keyframe<>(30, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Head snaps down on wind-up then settles a beat after the throw; the clip owns
                // look direction for its own duration, which is why this uses the ABSOLUTE override
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(3, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(-20.0f, 10.0f, 12.5f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(0.0f, -5.0f, -5.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, -5.0f, -5.0f), Easing.CUBIC),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(16, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Pupils widen and shift down through the whole throw, holding through the release
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(30, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(11, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(30, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(-0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(-0.1, 0.1, 0.0), Easing.CUBIC),
                                new Keyframe<>(30, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(11, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(20, new Vector3f(1.0f, 0.8f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(30, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .build();
    }

    /**
     * Looping idle while the hook is in the water. Gentle sway keeps the arms held
     * out and the body breathing; nose and head secondary motion adds subtle life.
     * Legs hold a planted, weight-shifted stance for the whole cycle.
     */
    public static KeyframeAnimation fishingLoop() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/loop"))
                .durationTicks(FISHING_WAIT_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Crossed arms hold the rod out with a subtle breathing sway
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-14.08f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-13.28f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-12.71f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-12.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-12.71f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-13.28f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(-16.34f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-17.04f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(26, RotationUtil.degrees(-17.44f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(28, RotationUtil.degrees(-17.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(-17.29f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(33, RotationUtil.degrees(-16.72f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-14.08f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.34, -1.0), Easing.LINEAR),
                                new Keyframe<>(5, new Vec3(0.0, 0.48, -1.0), Easing.LINEAR),
                                new Keyframe<>(8, new Vec3(0.0, 0.5, -1.0), Easing.LINEAR),
                                new Keyframe<>(10, new Vec3(0.0, 0.48, -1.0), Easing.LINEAR),
                                new Keyframe<>(16, new Vec3(0.0, 0.3, -1.0), Easing.LINEAR),
                                new Keyframe<>(23, new Vec3(0.0, 0.08, -1.0), Easing.LINEAR),
                                new Keyframe<>(25, new Vec3(0.0, 0.02, -1.0), Easing.LINEAR),
                                new Keyframe<>(28, new Vec3(0.0, 0.0, -1.0), Easing.LINEAR),
                                new Keyframe<>(30, new Vec3(0.0, 0.02, -1.0), Easing.LINEAR),
                                new Keyframe<>(36, new Vec3(0.0, 0.2, -1.0), Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(0.0, 0.34, -1.0), Easing.LINEAR)
                        ))
                        .build())
                // Nose micro-bob synced with the breath cycle for organic feel
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-1.25f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-3.54f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-4.6f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-5.55f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-6.35f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-6.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(-7.36f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(-7.36f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-6.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-6.35f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-5.55f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-4.6f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-2.42f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(1.04f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(2.1f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(3.05f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(26, RotationUtil.degrees(3.85f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(28, RotationUtil.degrees(4.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(29, RotationUtil.degrees(4.86f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(31, RotationUtil.degrees(4.86f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(33, RotationUtil.degrees(4.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(34, RotationUtil.degrees(3.85f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(35, RotationUtil.degrees(3.05f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(36, RotationUtil.degrees(2.1f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(39, RotationUtil.degrees(-0.08f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-1.25f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Torso very slightly leans forward and bobs — holding fishing stance
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(40, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // Head tracks the same breath cycle as the nose; the clip owns look direction
                // for the whole idle, which is why this is the ABSOLUTE override rather than additive
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(5.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(6.81f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(7.18f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(7.42f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(7.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(7.42f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(7.18f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(6.81f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(5.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(2.37f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(1.17f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(0.69f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(0.32f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(0.08f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(26, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(28, RotationUtil.degrees(0.08f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(29, RotationUtil.degrees(0.32f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.69f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(33, RotationUtil.degrees(1.74f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(39, RotationUtil.degrees(5.13f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(5.76f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(20, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(40, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Planted, weight-shifted stance held for the whole idle — odd non-round values
                // are authored, not approximations
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
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(4.992f, -4.9681f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, 1.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-2.5f, 7.476f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, -1.0), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Looping struggle animation while a fish is on the line. Aggressive pull-and-release
     * cycles; braced legs anchor the stance while the face holds a strained expression.
     */
    public static KeyframeAnimation fightFish() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/fight_fish"))
                .durationTicks(FIGHT_FISH_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-30.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(11, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(17, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(24, RotationUtil.degrees(-30.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(5, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(11, new Vec3(0.0, 1.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(17, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, 1.0, -2.0), Easing.CUBIC)
                        ))
                        .build())
                // Braced stance held for the whole fight
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, -2.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, 1.0), Easing.LINEAR)
                        ))
                        .build())
                // Nose rapid reactive judder — exaggerates the strain of reeling
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(1, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(2, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-7.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-11.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-12.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-11.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(-7.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(-7.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-11.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-12.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-11.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(-7.96f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(0.46f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(1, RotationUtil.degrees(-12.59f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(2, RotationUtil.degrees(-10.74f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-10.87f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-13.01f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-15.68f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-18.17f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-19.77f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(-20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-18.44f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(-16.24f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-13.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-11.56f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-10.2f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-11.35f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(-13.3f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-15.58f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-17.74f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-19.36f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(-20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(-19.26f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(-17.41f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.5, -1.0), Easing.LINEAR)
                        ))
                        .build())
                // Head snaps in counterpoint to torso; owns look direction for the whole fight
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-6.57f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(1, RotationUtil.degrees(-4.26f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(1.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(4.07f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(4.07f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(1.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(-4.26f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-6.57f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-6.57f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-4.26f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(1.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(4.07f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(4.07f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(1.76f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(-4.26f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(-6.57f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-6.57f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(6, new Vec3(0.0, -1.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.0, -1.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(18, new Vec3(0.0, -1.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.0, -1.4, 0.0), Easing.CUBIC)
                        ))
                        .build())
                // Braced, strained face held for the whole fight
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.5f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.15, 0.0), Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(0.1, 0.05, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(0.1, 0.15, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC)
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
                                new Keyframe<>(0, new Vector3f(1.0f, 1.5f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.15, 0.0), Easing.CUBIC),
                                new Keyframe<>(12, new Vec3(-0.1, 0.05, 0.0), Easing.CUBIC),
                                new Keyframe<>(24, new Vec3(-0.1, 0.15, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.7f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * One-shot reel-in snap. Short and punchy.
     */
    public static KeyframeAnimation reelIn() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/reel_in"))
                .durationTicks(REEL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-50.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(-1.11f, 0.0f, 0.24f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 1.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(3, new Vec3(0.0, 0.5, -0.5), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-7.5f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(0.0f, -7.5f, -1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, -2.0), Easing.CUBIC),
                                new Keyframe<>(10, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(0.0f, 10.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.LEG_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.0, 1.0), Easing.CUBIC),
                                new Keyframe<>(10, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(2, RotationUtil.degrees(3.7f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(5, RotationUtil.degrees(3.38f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(3, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.5, -1.0), Easing.CUBIC),
                                new Keyframe<>(10, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-16.57f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(4, RotationUtil.degrees(-1.04f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.4, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(3, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.15, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, new Vec3(0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(3, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, -1.0, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 1.5f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(3, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(-0.1, 0.15, 0.0), Easing.CUBIC),
                                new Keyframe<>(3, new Vec3(-0.1, 0.0, 0.0), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.7f, 1.0f), Easing.CUBIC),
                                new Keyframe<>(3, new Vector3f(1.0f, 1.0f, 1.0f), Easing.CUBIC)
                        ))
                        .build())
                .build();
    }

}
