package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EatingAnimations {

    public static final int EAT_LOOP_DURATION_TICKS = 16;

    private static final int BLEND_IN_TICKS = 4;
    private static final int BLEND_OUT_TICKS = 4;

    public static KeyframeAnimation eat() {
        // Loop closes on the same pose it opens so the seam is invisible at the repeat boundary.
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/eating/eat"))
                .durationTicks(EAT_LOOP_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, EatingPoses.ARMS_EAT_LOW, Easing.EASE_IN_OUT)
                .at(8, EatingPoses.ARMS_EAT_HIGH, Easing.EASE_IN_OUT)
                .at(EAT_LOOP_DURATION_TICKS, EatingPoses.ARMS_EAT_LOW, Easing.EASE_IN_OUT)
                .build();
    }

}
