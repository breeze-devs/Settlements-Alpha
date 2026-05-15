package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InteractAnimations {

    public static final int INTERACT_PEAK_TICK = 5;
    public static final int INTERACT_DURATION_TICKS = 12;

    public static KeyframeAnimation interact() {
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/interact/generic"))
                .durationTicks(INTERACT_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(1)
                .blendOutTicks(2)
                .at(0, InteractPoses.REST, Easing.EASE_IN)
                .at(INTERACT_PEAK_TICK, InteractPoses.INTERACT_PEAK, Easing.EASE_OUT)
                .at(INTERACT_DURATION_TICKS, InteractPoses.REST, Easing.LINEAR)
                .build();
    }

}
