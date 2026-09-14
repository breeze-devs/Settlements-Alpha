package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The ballista's animation clips.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaAnimations {

    public static final int WIND_DURATION_TICKS = 80;
    public static final int FIRE_DURATION_TICKS = 15;

    /**
     * The tick of fire on which the pusher reaches rest, which is when the shot leaves the machine.
     */
    public static final int FIRE_RELEASE_TICK = 2;

    /**
     * The ticks of wind on which a crank stroke begins to draw the pusher back, the first at the clip's start.
     */
    public static final List<Integer> WIND_STROKE_BEGIN_TICKS = List.of(0, 15, 33, 58);

    /**
     * The ticks of wind on which a crank stroke stops drawing and the machine holds before the next.
     */
    public static final List<Integer> WIND_STROKE_CATCH_TICKS = List.of(10, 28, 53, 75);

    public static KeyframeAnimation wind() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/ballista/wind"))
                .durationTicks(WIND_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                // No blending: the clip already starts on the pose it leaves and ends on the one it reaches
                .blendInTicks(0)
                .blendOutTicks(0)
                .track(rotation(BallistaAnimationTargets.LIMB_LEFT_OUTER_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, -8.13F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, -8.13F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, -16.25F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, -16.25F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, -24.38F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, -24.38F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, -32.5F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, -32.5F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.STRING_LEFT_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, 19.37F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, 19.37F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, 38.75F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, 38.75F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, 58.12F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, 58.12F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, 77.5F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, 77.5F, 0.0F))))
                .track(translation(BallistaAnimationTargets.PUSHER_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(10, new Vec3(0.0, 0.0, 5.0)),
                        at(15, new Vec3(0.0, 0.0, 5.0)),
                        at(28, new Vec3(0.0, 0.0, 10.0)),
                        at(33, new Vec3(0.0, 0.0, 10.0)),
                        at(53, new Vec3(0.0, 0.0, 15.0)),
                        at(58, new Vec3(0.0, 0.0, 15.0)),
                        at(75, new Vec3(0.0, 0.0, 20.0)),
                        at(80, new Vec3(0.0, 0.0, 20.0))))
                .track(rotation(BallistaAnimationTargets.CRANKSHAFT_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(-95.0F, 0.0F, 0.0F)),
                        at(15, RotationUtil.degrees(-90.0F, 0.0F, 0.0F)),
                        at(28, RotationUtil.degrees(-185.0F, 0.0F, 0.0F)),
                        at(33, RotationUtil.degrees(-180.0F, 0.0F, 0.0F)),
                        at(53, RotationUtil.degrees(-275.0F, 0.0F, 0.0F)),
                        at(58, RotationUtil.degrees(-270.0F, 0.0F, 0.0F)),
                        at(75, RotationUtil.degrees(-365.0F, 0.0F, 0.0F)),
                        at(80, RotationUtil.degrees(-360.0F, 0.0F, 0.0F))))
                .track(scale(BallistaAnimationTargets.CONNECTOR_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(10, new Vector3f(1.0F, 1.0F, 0.8F)),
                        at(15, new Vector3f(1.0F, 1.0F, 0.8F)),
                        at(28, new Vector3f(1.0F, 1.0F, 0.6F)),
                        at(33, new Vector3f(1.0F, 1.0F, 0.6F)),
                        at(53, new Vector3f(1.0F, 1.0F, 0.4F)),
                        at(58, new Vector3f(1.0F, 1.0F, 0.4F)),
                        at(75, new Vector3f(1.0F, 1.0F, 0.1F)),
                        at(80, new Vector3f(1.0F, 1.0F, 0.1F))))
                .track(rotation(BallistaAnimationTargets.GIMBAL_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(-1.0F, 0.0F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(28, RotationUtil.degrees(-1.0F, 0.0F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(53, RotationUtil.degrees(-1.0F, 0.0F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(75, RotationUtil.degrees(-1.0F, 0.0F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(translation(BallistaAnimationTargets.GIMBAL_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(10, new Vec3(0.0, 0.0, 0.2)),
                        at(15, new Vec3(0.0, 0.0, 0.0)),
                        at(28, new Vec3(0.0, 0.0, 0.2)),
                        at(33, new Vec3(0.0, 0.0, 0.0)),
                        at(53, new Vec3(0.0, 0.0, 0.2)),
                        at(58, new Vec3(0.0, 0.0, 0.0)),
                        at(75, new Vec3(0.0, 0.0, 0.2)),
                        at(80, new Vec3(0.0, 0.0, 0.0))))
                .track(rotation(BallistaAnimationTargets.LIMB_LEFT_TIP_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, -3.75F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, -3.75F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, -7.5F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, -7.5F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, -11.25F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, -11.25F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, -15.0F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, -15.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.LIMB_RIGHT_OUTER_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, 8.12F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, 8.12F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, 16.25F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, 16.25F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, 24.37F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, 24.37F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, 32.5F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, 32.5F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.LIMB_RIGHT_TIP_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, 3.75F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, 3.75F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, 7.5F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, 7.5F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, 11.25F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, 11.25F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, 15.0F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, 15.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.STRING_RIGHT_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(10, RotationUtil.degrees(0.0F, -19.38F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, -19.38F, 0.0F)),
                        at(28, RotationUtil.degrees(0.0F, -38.75F, 0.0F)),
                        at(33, RotationUtil.degrees(0.0F, -38.75F, 0.0F)),
                        at(53, RotationUtil.degrees(0.0F, -58.13F, 0.0F)),
                        at(58, RotationUtil.degrees(0.0F, -58.13F, 0.0F)),
                        at(75, RotationUtil.degrees(0.0F, -77.5F, 0.0F)),
                        at(80, RotationUtil.degrees(0.0F, -77.5F, 0.0F))))
                .build();
    }

    public static KeyframeAnimation fire() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/ballista/fire"))
                .durationTicks(FIRE_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                // No blending: the clip already starts on the pose it leaves and ends on the one it reaches
                .blendInTicks(0)
                .blendOutTicks(0)
                .track(rotation(BallistaAnimationTargets.LIMB_LEFT_OUTER_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, -32.5F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(3, RotationUtil.degrees(0.0F, 5.0F, 0.0F)),
                        at(5, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.STRING_LEFT_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 77.5F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(3, RotationUtil.degrees(0.0F, -7.5F, 0.0F)),
                        at(5, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(translation(BallistaAnimationTargets.PUSHER_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 20.0)),
                        at(2, new Vec3(0.0, 0.0, 0.0)),
                        at(3, new Vec3(0.0, 0.0, -1.0)),
                        at(5, new Vec3(0.0, 0.0, 0.0))))
                .track(rotation(BallistaAnimationTargets.CRANKSHAFT_ROTATION,
                        at(0, RotationUtil.degrees(-360.0F, 0.0F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(3, RotationUtil.degrees(10.0F, 0.0F, 0.0F)),
                        at(5, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(scale(BallistaAnimationTargets.CONNECTOR_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 0.1F)),
                        at(2, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(3, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(5, new Vector3f(1.0F, 1.0F, 1.0F))))
                .track(rotation(BallistaAnimationTargets.GIMBAL_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(4, RotationUtil.degrees(-5.0F, 0.0F, 0.0F)),
                        at(15, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(translation(BallistaAnimationTargets.GIMBAL_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(2, new Vec3(0.0, 0.3, 0.7)),
                        at(15, new Vec3(0.0, 0.0, 0.0))))
                .track(rotation(BallistaAnimationTargets.LIMB_LEFT_TIP_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, -15.0F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.LIMB_RIGHT_OUTER_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 32.5F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(3, RotationUtil.degrees(0.0F, -5.0F, 0.0F)),
                        at(5, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.LIMB_RIGHT_TIP_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, 15.0F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .track(rotation(BallistaAnimationTargets.STRING_RIGHT_ROTATION,
                        at(0, RotationUtil.degrees(0.0F, -77.5F, 0.0F)),
                        at(2, RotationUtil.degrees(0.0F, 0.0F, 0.0F)),
                        at(3, RotationUtil.degrees(0.0F, 7.5F, 0.0F)),
                        at(5, RotationUtil.degrees(0.0F, 0.0F, 0.0F))))
                .build();
    }

    @SafeVarargs
    private static AnimationTrack<Vector3f> rotation(AnimationTarget<Vector3f> target, Keyframe<Vector3f>... keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(List.of(keyframes)).build();
    }

    @SafeVarargs
    private static AnimationTrack<Vec3> translation(AnimationTarget<Vec3> target, Keyframe<Vec3>... keyframes) {
        return AnimationTrack.<Vec3>builder().target(target).keyframes(List.of(keyframes)).build();
    }

    @SafeVarargs
    private static AnimationTrack<Vector3f> scale(AnimationTarget<Vector3f> target, Keyframe<Vector3f>... keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(List.of(keyframes)).build();
    }

    private static <V> Keyframe<V> at(int tick, V value) {
        return new Keyframe<>(tick, value, Easing.LINEAR);
    }

}
