package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The canopy's own opening and closing clips, overlaid on the carry gesture in {@link UmbrellaCarryAnimations}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UmbrellaAnimations {

    public static final int DEPLOY_DURATION_TICKS = 10;
    public static final int UNDEPLOY_DURATION_TICKS = 10;

    public static KeyframeAnimation deploy() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/umbrella/deploy"))
                .durationTicks(DEPLOY_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(2)
                .blendOutTicks(2)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(rotation(UmbrellaAnimationTargets.EAST_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, 65.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(0.0f, 0.0f, -4.68f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.EAST_TRANSLATION,
                        at(0, new Vec3(0.6, 0.0, 0.0)),
                        at(5, new Vec3(0.0, 0.0, 0.0)),
                        at(7, new Vec3(-0.043, 0.0, 0.0)),
                        at(10, new Vec3(0.0, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.EAST_SCALE,
                        at(0, new Vector3f(1.5F, 1.0F, 0.3F)),
                        at(5, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(7, new Vector3f(0.964F, 1.0F, 1.05F)),
                        at(10, new Vector3f(1.0F, 1.0F, 1.0F))))
                .track(rotation(UmbrellaAnimationTargets.WEST_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, -65.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(0.0f, 0.0f, 4.68f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.WEST_TRANSLATION,
                        at(0, new Vec3(-0.6, 0.0, 0.0)),
                        at(5, new Vec3(0.0, 0.0, 0.0)),
                        at(7, new Vec3(0.043, 0.0, 0.0)),
                        at(10, new Vec3(0.0, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.WEST_SCALE,
                        at(0, new Vector3f(1.5F, 1.0F, 0.3F)),
                        at(5, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(7, new Vector3f(0.964F, 1.0F, 1.05F)),
                        at(10, new Vector3f(1.0F, 1.0F, 1.0F))))
                .track(rotation(UmbrellaAnimationTargets.NORTH_ROTATION,
                        at(0, RotationUtil.degrees(-65.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(4.68f, 0.0f, 0.0f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.NORTH_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.6)),
                        at(5, new Vec3(0.0, 0.0, 0.0)),
                        at(7, new Vec3(0.0, 0.0, -0.043)),
                        at(10, new Vec3(0.0, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.NORTH_SCALE,
                        at(0, new Vector3f(0.3F, 1.0F, 1.5F)),
                        at(5, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(7, new Vector3f(1.05F, 1.0F, 0.964F)),
                        at(10, new Vector3f(1.0F, 1.0F, 1.0F))))
                .track(rotation(UmbrellaAnimationTargets.SOUTH_ROTATION,
                        at(0, RotationUtil.degrees(65.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(-4.68f, 0.0f, 0.0f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.SOUTH_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, -0.6)),
                        at(5, new Vec3(0.0, 0.0, 0.0)),
                        at(7, new Vec3(0.0, 0.0, 0.043)),
                        at(10, new Vec3(0.0, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.SOUTH_SCALE,
                        at(0, new Vector3f(0.3F, 1.0F, 1.5F)),
                        at(5, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(7, new Vector3f(1.05F, 1.0F, 0.964F)),
                        at(10, new Vector3f(1.0F, 1.0F, 1.0F))))
                .build();
    }

    public static KeyframeAnimation undeploy() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/umbrella/undeploy"))
                .durationTicks(UNDEPLOY_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(0)
                .blendOutTicks(0)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(rotation(UmbrellaAnimationTargets.EAST_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, 65.0f)),
                        at(7, RotationUtil.degrees(0.0f, 0.0f, 69.68f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, 65.0f))))
                .track(translation(UmbrellaAnimationTargets.EAST_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(5, new Vec3(0.6, 0.0, 0.0)),
                        at(7, new Vec3(0.643, 0.0, 0.0)),
                        at(10, new Vec3(0.6, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.EAST_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(5, new Vector3f(1.5F, 1.0F, 0.3F)),
                        at(7, new Vector3f(1.536F, 1.0F, 0.25F)),
                        at(10, new Vector3f(1.5F, 1.0F, 0.3F))))
                .track(rotation(UmbrellaAnimationTargets.WEST_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(0.0f, 0.0f, -65.0f)),
                        at(7, RotationUtil.degrees(0.0f, 0.0f, -69.68f)),
                        at(10, RotationUtil.degrees(0.0f, 0.0f, -65.0f))))
                .track(translation(UmbrellaAnimationTargets.WEST_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(5, new Vec3(-0.6, 0.0, 0.0)),
                        at(7, new Vec3(-0.643, 0.0, 0.0)),
                        at(10, new Vec3(-0.6, 0.0, 0.0))))
                .track(scale(UmbrellaAnimationTargets.WEST_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(5, new Vector3f(1.5F, 1.0F, 0.3F)),
                        at(7, new Vector3f(1.536F, 1.0F, 0.25F)),
                        at(10, new Vector3f(1.5F, 1.0F, 0.3F))))
                .track(rotation(UmbrellaAnimationTargets.NORTH_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(-65.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(-69.68f, 0.0f, 0.0f)),
                        at(10, RotationUtil.degrees(-65.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.NORTH_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(5, new Vec3(0.0, 0.0, 0.6)),
                        at(7, new Vec3(0.0, 0.0, 0.643)),
                        at(10, new Vec3(0.0, 0.0, 0.6))))
                .track(scale(UmbrellaAnimationTargets.NORTH_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(5, new Vector3f(0.3F, 1.0F, 1.5F)),
                        at(7, new Vector3f(0.25F, 1.0F, 1.536F)),
                        at(10, new Vector3f(0.3F, 1.0F, 1.5F))))
                .track(rotation(UmbrellaAnimationTargets.SOUTH_ROTATION,
                        at(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f)),
                        at(5, RotationUtil.degrees(65.0f, 0.0f, 0.0f)),
                        at(7, RotationUtil.degrees(69.68f, 0.0f, 0.0f)),
                        at(10, RotationUtil.degrees(65.0f, 0.0f, 0.0f))))
                .track(translation(UmbrellaAnimationTargets.SOUTH_TRANSLATION,
                        at(0, new Vec3(0.0, 0.0, 0.0)),
                        at(5, new Vec3(0.0, 0.0, -0.6)),
                        at(7, new Vec3(0.0, 0.0, -0.643)),
                        at(10, new Vec3(0.0, 0.0, -0.6))))
                .track(scale(UmbrellaAnimationTargets.SOUTH_SCALE,
                        at(0, new Vector3f(1.0F, 1.0F, 1.0F)),
                        at(5, new Vector3f(0.3F, 1.0F, 1.5F)),
                        at(7, new Vector3f(0.25F, 1.0F, 1.536F)),
                        at(10, new Vector3f(0.3F, 1.0F, 1.5F))))
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
