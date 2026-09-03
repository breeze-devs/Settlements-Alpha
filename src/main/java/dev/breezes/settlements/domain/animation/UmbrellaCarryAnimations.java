package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.attachment.UmbrellaSlot;
import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The villager-side of the umbrella deploy/retract gesture: the crossed-arms tilt and the umbrella slot's
 * lift, overlaid with {@link UmbrellaAnimations}' canopy clips.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UmbrellaCarryAnimations {

    public static final int RAISE_DURATION_TICKS = 20;
    public static final int LOWER_DURATION_TICKS = 20;

    /**
     * Tick into {@link #raise()} at which the canopy clip starts.
     */
    public static final int RAISE_CANOPY_START_TICK = 6;
    /**
     * Tick into {@link #lower()} at which the canopy clip starts.
     */
    public static final int LOWER_CANOPY_START_TICK = 6;

    public static final float ARM_START_PITCH_DEGREES = 36.0F;
    public static final float SLOT_START_PITCH_DEGREES = 125.0F;
    public static final Vec3 START_OFFSET_BLOCKS = new Vec3(0.0D, -0.075D, 0.025D);

    public static KeyframeAnimation raise() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/umbrella/carry_raise"))
                .durationTicks(RAISE_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(0)
                .blendOutTicks(0)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(ARM_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(RAISE_DURATION_TICKS, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(SlotTargets.translation(UmbrellaSlot.UMBRELLA_LEFT))
                        .keyframes(List.of(
                                new Keyframe<>(0, START_OFFSET_BLOCKS, Easing.LINEAR),
                                new Keyframe<>(RAISE_DURATION_TICKS, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(SlotTargets.translation(UmbrellaSlot.UMBRELLA_RIGHT))
                        .keyframes(List.of(
                                new Keyframe<>(0, START_OFFSET_BLOCKS, Easing.LINEAR),
                                new Keyframe<>(RAISE_DURATION_TICKS, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(SlotTargets.rotation(UmbrellaSlot.UMBRELLA_LEFT))
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(SLOT_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(RAISE_DURATION_TICKS, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(SlotTargets.rotation(UmbrellaSlot.UMBRELLA_RIGHT))
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(SLOT_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(RAISE_DURATION_TICKS, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    public static KeyframeAnimation lower() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/umbrella/carry_lower"))
                .durationTicks(LOWER_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(0)
                .blendOutTicks(0)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(LOWER_DURATION_TICKS, RotationUtil.degrees(ARM_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(SlotTargets.translation(UmbrellaSlot.UMBRELLA_LEFT))
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(LOWER_DURATION_TICKS, START_OFFSET_BLOCKS, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(SlotTargets.translation(UmbrellaSlot.UMBRELLA_RIGHT))
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(LOWER_DURATION_TICKS, START_OFFSET_BLOCKS, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(SlotTargets.rotation(UmbrellaSlot.UMBRELLA_LEFT))
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(LOWER_DURATION_TICKS, RotationUtil.degrees(SLOT_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(SlotTargets.rotation(UmbrellaSlot.UMBRELLA_RIGHT))
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(LOWER_DURATION_TICKS, RotationUtil.degrees(SLOT_START_PITCH_DEGREES, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
