package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

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
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/fishing/cast_rod"))
                .durationTicks(CAST_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, FishingPoses.REST, Easing.EASE_OUT)
                .at(CAST_WIND_UP_TICK, FishingPoses.CAST_WIND_UP, Easing.EASE_IN)
                .at(CAST_IMPACT_TICK, FishingPoses.CAST_RELEASE, Easing.EASE_OUT)
                .at(CAST_SETTLE_TICK, FishingPoses.CAST_SETTLE, Easing.LINEAR)
                .at(CAST_DURATION_TICKS, FishingPoses.REST, Easing.LINEAR)
                .build();
    }

    public static KeyframeAnimation jigFight() {
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/fishing/jig_fight_rod"))
                .durationTicks(JIG_FIGHT_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, FishingPoses.JIG_LOW, Easing.LINEAR)
                .at(JIG_PAUSE_END_TICK, FishingPoses.JIG_LOW, Easing.EASE_OUT)
                .at(JIG_HIGH_TICK, FishingPoses.JIG_HIGH, Easing.EASE_IN)
                .at(JIG_FIGHT_DURATION_TICKS, FishingPoses.JIG_LOW, Easing.LINEAR)
                .build();
    }

    public static KeyframeAnimation reelYank() {
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/fishing/reel_yank_rod"))
                .durationTicks(REEL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, FishingPoses.JIG_LOW, Easing.EASE_IN)
                .at(REEL_IMPACT_TICK, FishingPoses.YANK_PEAK, Easing.EASE_OUT)
                .at(REEL_HOLD_END_TICK, FishingPoses.YANK_PEAK, Easing.LINEAR)
                .at(REEL_DURATION_TICKS, FishingPoses.REST, Easing.LINEAR)
                .build();
    }

}
