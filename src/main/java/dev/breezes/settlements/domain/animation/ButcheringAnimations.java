package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ButcheringAnimations {

    public static final int SWING_DURATION_TICKS = 12;
    public static final int SWING_RAISE_TICKS = 5;
    public static final int SWING_IMPACT_TICKS = 8;

    private static final int BLEND_TICKS = 3;

    public static KeyframeAnimation swingHeavyAxe() {
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/butchering/swing_heavy_axe"))
                .durationTicks(SWING_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(1)
                .blendOutTicks(BLEND_TICKS)
                .at(0, ButcheringPoses.ARMS_REST, Easing.EASE_IN)
                .at(SWING_RAISE_TICKS, ButcheringPoses.ARMS_RAISED, Easing.EASE_IN)
                .at(SWING_IMPACT_TICKS, ButcheringPoses.ARMS_IMPACT, Easing.EASE_OUT)
                .at(SWING_DURATION_TICKS, ButcheringPoses.ARMS_REST, Easing.EASE_OUT)
                .build();
    }

}
