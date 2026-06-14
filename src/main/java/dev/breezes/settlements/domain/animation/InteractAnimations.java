package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.joml.Vector3f;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InteractAnimations {

    public static final int INTERACT_PEAK_TICK = 5;
    public static final int INTERACT_DURATION_TICKS = 12;

    public static KeyframeAnimation interact() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/interact/generic"))
                .durationTicks(INTERACT_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(1)
                .blendOutTicks(2)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.EASE_IN),
                                new Keyframe<>(INTERACT_PEAK_TICK, RotationUtil.degrees(-30.0F, 0.0F, 0.0F), Easing.EASE_OUT),
                                new Keyframe<>(INTERACT_DURATION_TICKS, RotationUtil.degrees(0.0F, 0.0F, 0.0F), Easing.LINEAR)))
                        .build())
                .build();
    }

}
