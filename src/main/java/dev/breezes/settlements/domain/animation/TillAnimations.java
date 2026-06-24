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
public final class TillAnimations {

    /**
     * Total clip length: 1.25s × 20 ticks/s = 25 ticks.
     */
    public static final int TILL_DURATION_TICKS = 25;
    public static final int ARM_RETURN_TO_CROSSED_AT_TICK = 20;

    /**
     * The hoe-down frame that the behavior uses to convert the ground block to farmland.
     * Corresponds to the down-stroke around 0.5 s (tick 10) — arms swing down from ~78 to ~24 degrees.
     */
    public static final int TILL_IMPACT_TICK = 10;

    public static KeyframeAnimation till() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/farming/till"))
                .durationTicks(TILL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(2)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .armConfigurationAt(ARM_RETURN_TO_CROSSED_AT_TICK, ArmConfiguration.BOTH_CROSSED)
                // arm_straight_left ROTATION
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-70.0f, 20.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-77.5f, 20.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-24.1436f, 26.2132f, 8.5267f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(-31.6436f, 26.2132f, 8.5267f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                // arm_straight_left POSITION — Y values are 0 throughout, no negation effect
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_LEFT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(5, new Vec3(0.25, 0.0, 1.0), Easing.LINEAR),
                                new Keyframe<>(23, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                // arm_straight_right ROTATION
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-71.0546f, -29.5016f, -3.4396f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-78.5546f, -29.5016f, -3.4396f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-13.5335f, -35.7485f, -12.7245f), Easing.LINEAR),
                                new Keyframe<>(17, RotationUtil.degrees(-26.0335f, -35.7485f, -12.7245f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                // arm_straight_right POSITION — Y values are 0 throughout, no negation effect
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(5, new Vec3(-0.25, 0.0, -2.0), Easing.LINEAR),
                                new Keyframe<>(23, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                // leg_left ROTATION
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                // nose ROTATION
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-17.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-17.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                // torso ROTATION
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(32.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(32.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)))
                        .build())
                // torso POSITION — negate Y: posVec(0,-1.5,0) → Vec3(0.0, 1.5, 0.0)
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(5, new Vec3(0.0, 1.5, 0.0), Easing.LINEAR),
                                new Keyframe<>(16, new Vec3(0.0, 1.5, 0.0), Easing.LINEAR),
                                new Keyframe<>(25, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                .build();
    }

}
