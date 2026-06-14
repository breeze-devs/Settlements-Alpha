package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.joml.Vector3f;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FishingAnimations {

    public static final int CAST_IMPACT_TICK = 6;
    public static final int REEL_IMPACT_TICK = 4;

    public static final int CAST_DURATION_TICKS = 20;
    private static final int CAST_WIND_UP_TICK = 5;
    private static final int CAST_SETTLE_TICK = 10;

    private static final int JIG_PAUSE_END_TICK = 5;
    private static final int JIG_HIGH_TICK = 35;
    public static final int JIG_FIGHT_DURATION_TICKS = 40;

    public static final int REEL_DURATION_TICKS = 14;
    private static final int REEL_HOLD_END_TICK = 8;

    private static final int BLEND_IN_TICKS = 2;
    private static final int BLEND_OUT_TICKS = 3;

    public static KeyframeAnimation cast() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/cast_rod"))
                .durationTicks(CAST_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(arms(List.of(
                        key(0, 0.0F, Easing.EASE_OUT),
                        key(CAST_WIND_UP_TICK, -40.0F, Easing.EASE_IN),
                        key(CAST_IMPACT_TICK, 30.0F, Easing.EASE_OUT),
                        key(CAST_SETTLE_TICK, 12.0F, Easing.LINEAR),
                        key(CAST_DURATION_TICKS, 0.0F, Easing.LINEAR))))
                .build();
    }

    public static KeyframeAnimation jigFight() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/jig_fight_rod"))
                .durationTicks(JIG_FIGHT_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(arms(List.of(
                        key(0, 18.0F, Easing.LINEAR),
                        key(JIG_PAUSE_END_TICK, 18.0F, Easing.EASE_OUT),
                        key(JIG_HIGH_TICK, -15.0F, Easing.EASE_IN),
                        key(JIG_FIGHT_DURATION_TICKS, 18.0F, Easing.LINEAR))))
                .build();
    }

    public static KeyframeAnimation reelYank() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/reel_yank_rod"))
                .durationTicks(REEL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(arms(List.of(
                        key(0, 18.0F, Easing.EASE_IN),
                        key(REEL_IMPACT_TICK, -55.0F, Easing.EASE_OUT),
                        key(REEL_HOLD_END_TICK, -55.0F, Easing.LINEAR),
                        key(REEL_DURATION_TICKS, 0.0F, Easing.LINEAR))))
                .build();
    }

    private static AnimationTrack<Vector3f> arms(List<Keyframe<Float>> pitchKeyframes) {
        return AnimationTrack.<Vector3f>builder()
                .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                .keyframes(pitchKeyframes.stream()
                        .map(keyframe -> new Keyframe<>(
                                keyframe.tick(),
                                RotationUtil.degrees(keyframe.value(), 0.0F, 0.0F),
                                keyframe.easingToNext()))
                        .toList())
                .build();
    }

    private static Keyframe<Float> key(int tick, float pitchDegrees, Easing easingToNext) {
        return new Keyframe<>(tick, pitchDegrees, easingToNext);
    }

}
