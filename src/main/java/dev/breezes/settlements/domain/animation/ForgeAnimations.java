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
                .blendInTicks(2)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-70.0f, 15.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-70.0f, -15.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-150.0f, -15.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-70.0f, -15.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(14, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(14, new Vec3(0.0, 0.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(20, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(18.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(22.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(8, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(12, new Vec3(0.0, 0.2, 0.0), Easing.LINEAR),
                                new Keyframe<>(20, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
