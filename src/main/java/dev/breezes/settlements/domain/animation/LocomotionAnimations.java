package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocomotionAnimations {

    public static KeyframeAnimation stroll() {
        return gait("stroll", 24,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, keys(
                        r(0, 0, 3, 1), r(6, 0, 0, 0), r(12, 0, -3, -1), r(18, 0, 0, 0), r(24, 0, 3, 1))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, keys(
                        r(0, 15, 0, 0), r(6, 0, 0, 0), r(12, -15, 0, 0), r(18, 0, 0, 0), r(24, 15, 0, 0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, keys(
                        r(0, -15, 0, 0), r(6, 0, 0, 0), r(12, 15, 0, 0), r(18, 0, 0, 0), r(24, -15, 0, 0))),
                rotations(AnimationTargets.NOSE_ROTATION, keys(
                        r(0, 0, 0, 0), r(3, 2, 0, 0), r(6, 0, 0, 0), r(9, -2, 0, 0), r(12, 0, 0, 0), r(15, 2, 0, 0), r(18, 0, 0, 0), r(21, -2, 0, 0), r(24, 0, 0, 0))),
                translations(AnimationTargets.NOSE_TRANSLATION, keys(
                        p(0, 0, 0, 0), p(3, 0, -0.08, 0), p(6, 0, 0, 0), p(9, 0, 0.08, 0), p(12, 0, 0, 0), p(15, 0, -0.08, 0), p(18, 0, 0, 0), p(21, 0, 0.08, 0), p(24, 0, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, keys(
                        r(0, -3, -2, 0), r(6, -1, 0, 0), r(12, -3, 2, 0), r(18, -1, 0, 0), r(24, -3, -2, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, keys(
                        p(0, 0, 0.2, 0), p(6, 0, 0, 0), p(12, 0, 0.2, 0), p(18, 0, 0, 0), p(24, 0, 0.2, 0))));
    }

    public static KeyframeAnimation walk() {
        return gait("walk", 20,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, keys(
                        r(0, 0, 4, 1), r(3, 0, 2, 0.5F), r(5, 0, -2, -0.5F), r(8, 0, -4, -1), r(10, 0, -2, -0.5F), r(13, 0, 2, 0.5F), r(15, 0, 4, 1), r(18, 0, 2, 0.5F), r(20, 0, 4, 1))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, keys(
                        r(0, 22, 0, 0), r(5, 0, 0, 0), r(10, -22, 0, 0), r(15, 0, 0, 0), r(20, 22, 0, 0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, keys(
                        r(0, -22, 0, 0), r(5, 0, 0, 0), r(10, 22, 0, 0), r(15, 0, 0, 0), r(20, -22, 0, 0))),
                rotations(AnimationTargets.NOSE_ROTATION, keys(
                        r(0, 0, 0, 0), r(3, 4, 0, 0), r(5, 0, 0, 0), r(8, -4, 0, 0), r(10, 0, 0, 0), r(13, 4, 0, 0), r(15, 0, 0, 0), r(18, -4, 0, 0), r(20, 0, 0, 0))),
                translations(AnimationTargets.NOSE_TRANSLATION, keys(
                        p(0, 0, 0, 0), p(3, 0, -0.15, -0.05), p(5, 0, 0, 0), p(8, 0, 0.15, 0.05), p(10, 0, 0, 0), p(13, 0, -0.15, -0.05), p(15, 0, 0, 0), p(18, 0, 0.15, 0.05), p(20, 0, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, keys(
                        r(0, 4, -3, 0), r(3, 3, -1.5F, 0), r(5, 2, 0, 0), r(8, 3, 1.5F, 0), r(10, 4, 3, 0), r(13, 3, 1.5F, 0), r(15, 2, 0, 0), r(18, 3, -1.5F, 0), r(20, 4, -3, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, keys(
                        p(0, 0, 0.3, 0), p(3, 0, 0.15, 0), p(5, 0, 0, 0), p(8, 0, 0.15, 0), p(10, 0, 0.3, 0), p(13, 0, 0.15, 0), p(15, 0, 0, 0), p(18, 0, 0.15, 0), p(20, 0, 0.3, 0))));
    }

    public static KeyframeAnimation jog() {
        return gait("jog", 16,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, keys(
                        r(0, 0, 5, 1.5F), r(4, 0, 0, 0), r(8, 0, -5, -1.5F), r(12, 0, 0, 0), r(16, 0, 5, 1.5F))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, keys(
                        r(0, 35, 0, 0), r(4, 0, 0, 0), r(8, -35, 0, 0), r(12, 0, 0, 0), r(16, 35, 0, 0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, keys(
                        r(0, -35, 0, 0), r(4, 0, 0, 0), r(8, 35, 0, 0), r(12, 0, 0, 0), r(16, -35, 0, 0))),
                rotations(AnimationTargets.NOSE_ROTATION, keys(
                        r(0, 0, 0, 0), r(2, 4, 0, 0), r(4, 0, 0, 0), r(6, -4, 0, 0), r(8, 0, 0, 0), r(10, 4, 0, 0), r(12, 0, 0, 0), r(14, -4, 0, 0), r(16, 0, 0, 0))),
                translations(AnimationTargets.NOSE_TRANSLATION, keys(
                        p(0, 0, 0, 0), p(2, 0, -0.15, -0.05), p(4, 0, 0, 0), p(6, 0, 0.15, 0.05), p(8, 0, 0, 0), p(10, 0, -0.15, -0.05), p(12, 0, 0, 0), p(14, 0, 0.15, 0.05), p(16, 0, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, keys(
                        r(0, 7, -4, 0), r(4, 5, 0, 0), r(8, 7, 4, 0), r(12, 5, 0, 0), r(16, 7, -4, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, keys(
                        p(0, 0, 0.4, 0), p(4, 0, 0, 0), p(8, 0, 0.4, 0), p(12, 0, 0, 0), p(16, 0, 0.4, 0))));
    }

    public static KeyframeAnimation run() {
        return gait("run", 12,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, keys(
                        r(0, 0, 6, 2), r(3, 0, 0, 0), r(6, 0, -6, -2), r(9, 0, 0, 0), r(12, 0, 6, 2))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, keys(
                        r(0, 45, 0, 0), r(3, 0, 0, 0), r(6, -45, 0, 0), r(9, 0, 0, 0), r(12, 45, 0, 0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, keys(
                        r(0, -45, 0, 0), r(3, 0, 0, 0), r(6, 45, 0, 0), r(9, 0, 0, 0), r(12, -45, 0, 0))),
                rotations(AnimationTargets.NOSE_ROTATION, keys(
                        r(0, 0, 0, 0), r(2, 5, 0, 0), r(3, 0, 0, 0), r(5, -5, 0, 0), r(6, 0, 0, 0), r(8, 5, 0, 0), r(9, 0, 0, 0), r(11, -5, 0, 0), r(12, 0, 0, 0))),
                translations(AnimationTargets.NOSE_TRANSLATION, keys(
                        p(0, 0, 0, 0), p(2, 0, -0.2, -0.06), p(3, 0, 0, 0), p(5, 0, 0.2, 0.06), p(6, 0, 0, 0), p(8, 0, -0.2, -0.06), p(9, 0, 0, 0), p(11, 0, 0.2, 0.06), p(12, 0, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, keys(
                        r(0, 11, -6, 0), r(3, 9, 0, 0), r(6, 11, 6, 0), r(9, 9, 0, 0), r(12, 11, -6, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, keys(
                        p(0, 0, 0.5, 0), p(3, 0, 0, 0), p(6, 0, 0.5, 0), p(9, 0, 0, 0), p(12, 0, 0.5, 0))));
    }

    @SafeVarargs
    private static KeyframeAnimation gait(@Nonnull String name, int durationTicks, AnimationTrack<?>... tracks) {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/locomotion/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(3)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(tracks[0])
                .track(tracks[1])
                .track(tracks[2])
                .track(tracks[3])
                .track(tracks[4])
                .track(tracks[5])
                .track(tracks[6])
                .build();
    }

    private static AnimationTrack<Vector3f> rotations(AnimationTarget<Vector3f> target, List<Keyframe<Vector3f>> keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(keyframes).build();
    }

    private static AnimationTrack<Vec3> translations(AnimationTarget<Vec3> target, List<Keyframe<Vec3>> keyframes) {
        return AnimationTrack.<Vec3>builder().target(target).keyframes(keyframes).build();
    }

    @SafeVarargs
    private static <V> List<Keyframe<V>> keys(Keyframe<V>... keyframes) {
        return List.of(keyframes);
    }

    /**
     * Rotation
     */
    private static Keyframe<Vector3f> r(int tick, float pitch, float yaw, float roll) {
        return new Keyframe<>(tick, RotationUtil.degrees(pitch, yaw, roll), Easing.LINEAR);
    }

    /**
     * Translation
     */
    private static Keyframe<Vec3> p(int tick, double x, double y, double z) {
        return new Keyframe<>(tick, x == 0.0D && y == 0.0D && z == 0.0D ? Vec3.ZERO : new Vec3(x, y, z), Easing.LINEAR);
    }

}
