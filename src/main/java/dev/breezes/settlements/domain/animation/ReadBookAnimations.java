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
public final class ReadBookAnimations {

    public static final int READ_BOOK_DURATION_TICKS = 60;

    private static final int BLEND_IN_TICKS = 4;
    private static final int BLEND_OUT_TICKS = 4;

    /**
     * A sustained page-reading cycle: the book drifts down as the eyes track left to right, then resets
     * as the gaze snaps back to the start of the next line.
     */
    public static KeyframeAnimation readBook() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/librarian/read_book"))
                .durationTicks(READ_BOOK_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(rotation(AnimationTargets.ARMS_CROSSED_ROTATION, List.of(
                        key(0, rotation(-8.8F, 0.0F, 0.0F), Easing.LINEAR),
                        key(5, rotation(-7.87F, 0.0F, 0.0F), Easing.LINEAR),
                        key(8, rotation(-7.54F, 0.0F, 0.0F), Easing.LINEAR),
                        key(12, rotation(-7.54F, 0.0F, 0.0F), Easing.LINEAR),
                        key(15, rotation(-7.87F, 0.0F, 0.0F), Easing.LINEAR),
                        key(18, rotation(-8.44F, 0.0F, 0.0F), Easing.LINEAR),
                        key(30, rotation(-11.2F, 0.0F, 0.0F), Easing.LINEAR),
                        key(35, rotation(-12.13F, 0.0F, 0.0F), Easing.LINEAR),
                        key(38, rotation(-12.46F, 0.0F, 0.0F), Easing.LINEAR),
                        key(42, rotation(-12.46F, 0.0F, 0.0F), Easing.LINEAR),
                        key(45, rotation(-12.13F, 0.0F, 0.0F), Easing.LINEAR),
                        key(48, rotation(-11.56F, 0.0F, 0.0F), Easing.LINEAR),
                        key(60, rotation(-8.8F, 0.0F, 0.0F), Easing.LINEAR))))
                .track(translation(AnimationTargets.ARMS_CROSSED_TRANSLATION, List.of(
                        key(0, new Vec3(0.0D, 1.95D, -1.0D), Easing.LINEAR),
                        key(8, new Vec3(0.0D, 2.0D, -1.0D), Easing.LINEAR),
                        key(15, new Vec3(0.0D, 1.99D, -1.0D), Easing.LINEAR),
                        key(23, new Vec3(0.0D, 1.92D, -1.0D), Easing.LINEAR),
                        key(32, new Vec3(0.0D, 1.84D, -1.0D), Easing.LINEAR),
                        key(38, new Vec3(0.0D, 1.8D, -1.0D), Easing.LINEAR),
                        key(45, new Vec3(0.0D, 1.81D, -1.0D), Easing.LINEAR),
                        key(53, new Vec3(0.0D, 1.88D, -1.0D), Easing.LINEAR),
                        key(60, new Vec3(0.0D, 1.95D, -1.0D), Easing.LINEAR))))
                .track(rotation(AnimationTargets.BODY_ROTATION, List.of(
                        key(0, rotation(0.0F, 0.0F, 0.0F), Easing.CUBIC),
                        key(13, rotation(-1.35F, -2.0F, 0.0F), Easing.CUBIC),
                        key(30, rotation(-3.0F, 0.0F, 0.0F), Easing.CUBIC),
                        key(47, rotation(-1.35F, 2.0F, 0.0F), Easing.CUBIC),
                        key(60, rotation(0.0F, 0.0F, 0.0F), Easing.CUBIC))))
                .track(rotation(AnimationTargets.HEAD_ROTATION_OVERRIDE, List.of(
                        key(0, rotation(19.63F, 0.0F, 0.0F), Easing.LINEAR),
                        key(3, rotation(19.96F, 0.0F, 0.0F), Easing.LINEAR),
                        key(7, rotation(19.96F, 0.0F, 0.0F), Easing.LINEAR),
                        key(10, rotation(19.63F, 0.0F, 0.0F), Easing.LINEAR),
                        key(13, rotation(19.06F, 0.0F, 0.0F), Easing.LINEAR),
                        key(25, rotation(16.3F, 0.0F, 0.0F), Easing.LINEAR),
                        key(30, rotation(15.37F, 0.0F, 0.0F), Easing.LINEAR),
                        key(33, rotation(15.04F, 0.0F, 0.0F), Easing.LINEAR),
                        key(37, rotation(15.04F, 0.0F, 0.0F), Easing.LINEAR),
                        key(40, rotation(15.37F, 0.0F, 0.0F), Easing.LINEAR),
                        key(43, rotation(15.94F, 0.0F, 0.0F), Easing.LINEAR),
                        key(55, rotation(18.7F, 0.0F, 0.0F), Easing.LINEAR),
                        key(60, rotation(19.63F, 0.0F, 0.0F), Easing.LINEAR))))
                .track(rotation(AnimationTargets.NOSE_ROTATION, List.of(
                        key(0, rotation(-7.5F, 0.0F, 0.0F), Easing.CUBIC),
                        key(30, rotation(0.0F, 0.0F, 0.0F), Easing.CUBIC),
                        key(60, rotation(-7.5F, 0.0F, 0.0F), Easing.CUBIC))))
                .track(translation(AnimationTargets.PUPIL_LEFT_TRANSLATION, List.of(
                        key(0, new Vec3(0.9D, 0.1D, 0.0D), Easing.CUBIC),
                        key(20, new Vec3(0.1D, 0.1D, 0.0D), Easing.CUBIC),
                        key(30, new Vec3(0.1D, 0.1D, 0.0D), Easing.CUBIC),
                        key(50, new Vec3(0.9D, 0.1D, 0.0D), Easing.CUBIC))))
                .track(translation(AnimationTargets.PUPIL_RIGHT_TRANSLATION, List.of(
                        key(0, new Vec3(-0.1D, 0.1D, 0.0D), Easing.CUBIC),
                        key(20, new Vec3(-0.9D, 0.1D, 0.0D), Easing.CUBIC),
                        key(30, new Vec3(-0.9D, 0.1D, 0.0D), Easing.CUBIC),
                        key(50, new Vec3(-0.1D, 0.1D, 0.0D), Easing.CUBIC))))
                .track(scale(AnimationTargets.PUPIL_LEFT_SCALE, List.of(
                        key(0, new Vector3f(1.0F, 0.8F, 1.0F), Easing.CUBIC))))
                .track(scale(AnimationTargets.PUPIL_RIGHT_SCALE, List.of(
                        key(0, new Vector3f(1.0F, 0.8F, 1.0F), Easing.CUBIC))))
                .track(rotation(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, List.of(
                        key(0, rotation(0.0F, -7.5F, -1.0F), Easing.LINEAR))))
                .track(rotation(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, List.of(
                        key(0, rotation(0.0F, 10.0F, 1.0F), Easing.LINEAR))))
                .build();
    }

    private static AnimationTrack<Vector3f> rotation(@Nonnull AnimationTarget<Vector3f> target,
                                                     @Nonnull List<Keyframe<Vector3f>> keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(keyframes).build();
    }

    private static AnimationTrack<Vec3> translation(@Nonnull AnimationTarget<Vec3> target,
                                                    @Nonnull List<Keyframe<Vec3>> keyframes) {
        return AnimationTrack.<Vec3>builder().target(target).keyframes(keyframes).build();
    }

    private static AnimationTrack<Vector3f> scale(@Nonnull AnimationTarget<Vector3f> target,
                                                  @Nonnull List<Keyframe<Vector3f>> keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(keyframes).build();
    }

    private static Vector3f rotation(float pitchDegrees, float yawDegrees, float rollDegrees) {
        return RotationUtil.degrees(pitchDegrees, yawDegrees, rollDegrees);
    }

    private static <V> Keyframe<V> key(int tick, @Nonnull V value, @Nonnull Easing easingToNext) {
        return new Keyframe<>(tick, value, easingToNext);
    }

}
