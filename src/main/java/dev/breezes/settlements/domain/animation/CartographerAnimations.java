package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartographerAnimations {

    public static final int MARK_DURATION_TICKS = 40;

    private static final int BLEND_IN_TICKS = 2;
    private static final int BLEND_OUT_TICKS = 3;

    // Spyglass sweep keyframe ticks
    public static final int SURVEY_DURATION_TICKS = 160;
    private static final int SURVEY_RAISE_TICK = 12;
    private static final int SURVEY_LEFT_TICK = 42;
    private static final int SURVEY_RIGHT_TICK = 92;
    private static final int SURVEY_CENTER_TICK = 148;

    // Map marking keyframe ticks
    private static final int MAP_RAISE_TICK = 8;
    private static final int MAP_MARK_TICK_1 = 16;
    private static final int MAP_HOLD_TICK = 24;
    private static final int MAP_MARK_TICK_2 = 32;

    public static KeyframeAnimation surveyWithSpyglass() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/cartographer/survey_with_spyglass"))
                .durationTicks(SURVEY_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(rotation(AnimationTargets.ARMS_CROSSED_ROTATION, List.of(
                        key(0, rotation(0.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(SURVEY_RAISE_TICK, rotation(-60.0F, 0.0F, 0.0F), Easing.EASE_IN),
                        key(SURVEY_LEFT_TICK, rotation(-60.0F, -25.0F, 0.0F), Easing.EASE_IN_OUT),
                        key(SURVEY_RIGHT_TICK, rotation(-60.0F, 25.0F, 0.0F), Easing.EASE_IN_OUT),
                        key(SURVEY_CENTER_TICK, rotation(-60.0F, 0.0F, 0.0F), Easing.EASE_IN),
                        key(SURVEY_DURATION_TICKS, rotation(0.0F, 0.0F, 0.0F), Easing.LINEAR))))
                .track(translation(AnimationTargets.ARMS_CROSSED_TRANSLATION, List.of(
                        key(0, Vec3.ZERO, Easing.EASE_OUT),
                        key(SURVEY_RAISE_TICK, new Vec3(0.0D, -2.2D, -0.3D), Easing.EASE_IN),
                        key(SURVEY_DURATION_TICKS, Vec3.ZERO, Easing.LINEAR))))
                .track(rotation(AnimationTargets.HEAD_ROTATION_OVERRIDE, List.of(
                        key(0, rotation(0.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(SURVEY_RAISE_TICK, rotation(-10.0F, 0.0F, 0.0F), Easing.EASE_IN),
                        key(SURVEY_LEFT_TICK, rotation(-10.0F, -25.0F, 0.0F), Easing.EASE_IN_OUT),
                        key(SURVEY_RIGHT_TICK, rotation(-10.0F, 25.0F, 0.0F), Easing.EASE_IN_OUT),
                        key(SURVEY_CENTER_TICK, rotation(-10.0F, 0.0F, 0.0F), Easing.EASE_IN),
                        key(SURVEY_DURATION_TICKS, rotation(0.0F, 0.0F, 0.0F), Easing.LINEAR))))
                .build();
    }

    public static KeyframeAnimation markMap() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/cartographer/mark_map"))
                .durationTicks(MARK_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(rotation(AnimationTargets.ARMS_CROSSED_ROTATION, List.of(
                        key(0, rotation(0.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(MAP_RAISE_TICK, rotation(10.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(MARK_DURATION_TICKS, rotation(0.0F, 0.0F, 0.0F), Easing.LINEAR))))
                .track(rotation(AnimationTargets.HEAD_ROTATION_OVERRIDE, List.of(
                        key(0, rotation(0.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(MAP_RAISE_TICK, rotation(25.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                        key(MARK_DURATION_TICKS, rotation(0.0F, 0.0F, 0.0F), Easing.LINEAR))))
                .track(translation(AnimationTargets.ARMS_CROSSED_TRANSLATION, List.of(
                        key(0, Vec3.ZERO, Easing.EASE_OUT),
                        key(MAP_RAISE_TICK, Vec3.ZERO, Easing.EASE_OUT),
                        key(MAP_MARK_TICK_1, new Vec3(0.0D, 0.0D, -0.6D), Easing.EASE_IN),
                        key(MAP_HOLD_TICK, Vec3.ZERO, Easing.EASE_OUT),
                        key(MAP_MARK_TICK_2, new Vec3(0.0D, 0.0D, -0.6D), Easing.EASE_IN),
                        key(MARK_DURATION_TICKS, Vec3.ZERO, Easing.LINEAR))))
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

    private static Vector3f rotation(float pitchDegrees, float yawDegrees, float rollDegrees) {
        return RotationUtil.degrees(pitchDegrees, yawDegrees, rollDegrees);
    }

    private static <V> Keyframe<V> key(int tick, @Nonnull V value, @Nonnull Easing easingToNext) {
        return new Keyframe<>(tick, value, easingToNext);
    }

}
