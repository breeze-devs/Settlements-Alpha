package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.joml.Vector3f;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EatingAnimations {

    public static final int EAT_LOOP_DURATION_TICKS = 16;

    private static final int BLEND_IN_TICKS = 4;
    private static final int BLEND_OUT_TICKS = 4;

    public static KeyframeAnimation eat() {
        // Loop closes on the same pose it opens so the seam is invisible at the repeat boundary.
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/eating/eat"))
                .durationTicks(EAT_LOOP_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-50.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(8, RotationUtil.degrees(-60.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT),
                                new Keyframe<>(EAT_LOOP_DURATION_TICKS, RotationUtil.degrees(-50.0F, 0.0F, 0.0F), Easing.EASE_IN_OUT)))
                        .build())
                .build();
    }

}
