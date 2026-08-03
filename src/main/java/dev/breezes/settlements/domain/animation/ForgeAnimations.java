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
public final class ForgeAnimations {

    public static final int STRIKE_DURATION_TICKS = 20;
    public static final int STRIKE_PEAK_TICK = 8;

    public static KeyframeAnimation forge() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/forge/work_anvil"))
                .durationTicks(STRIKE_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(4)
                .blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-100.0f, 20.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(-105.0f, 20.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(-100.0f, 20.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-80.0f, -15.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(-160.0f, -15.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(13, RotationUtil.degrees(-80.0f, -15.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.55f, -0.94f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(1, RotationUtil.degrees(0.39f, -0.45f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(2, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-0.59f, 0.38f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-4.78f, 2.28f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-5.83f, 2.67f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-6.7f, 2.94f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(-7.5f, 3.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-6.84f, 2.28f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(-5.22f, 0.96f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-3.18f, -0.6f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-1.26f, -2.04f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(0.56f, -3.56f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(0.28f, -3.44f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(0.0f, -3.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(0.22f, -2.36f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(0.52f, -1.44f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(0.55f, -0.94f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // The ticks this dense sequence skips are deliberate rather than gaps to be filled in:
                // the curve interpolates straight across them.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-6.15f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(2, RotationUtil.degrees(-2.78f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-1.25f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(3.66f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(4.42f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(5.33f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(5.35f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(11, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(3.04f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-0.28f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(-7.64f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-11.48f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(-11.3f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(-7.73f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-6.15f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(8, RotationUtil.degrees(15.0f, 5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(12, RotationUtil.degrees(20.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(14, RotationUtil.degrees(20.0f, -5.0f, 0.0f), Easing.CUBIC),
                                new Keyframe<>(20, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.CUBIC)
                        ))
                        .build())
                // The brow stays neutral until the furrow appears late in the swing, so this track opens
                // at that tick instead of padding the earlier ones.
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(13, Vec3.ZERO, Easing.CUBIC),
                                new Keyframe<>(14, new Vec3(0.0, -0.3, 0.0), Easing.CUBIC),
                                new Keyframe<>(20, Vec3.ZERO, Easing.CUBIC)
                        ))
                        .build())
                // Static squint and splayed-leg poses held for the whole clip.
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_LEFT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.6f, 1.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.1, 0.0, 0.0), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.EYEBALL_RIGHT_SCALE)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vector3f(1.0f, 0.6f, 1.0f), Easing.LINEAR)
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
