package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;

import java.util.List;

/**
 * Builders for the throwaway clips the animation tests fold together. Every magnitude a test needs is
 * passed in by that test, so no assertion ends up restating an authored constant.
 */
final class AnimationTestClips {

    /**
     * One-shot clip holding {@code value} on {@link AnimationTestTargets#FLOAT} for its whole length.
     */
    static KeyframeAnimation held(String name, int blendInTicks, int durationTicks, int blendOutTicks, float value) {
        return held(name, blendInTicks, durationTicks, blendOutTicks, AnimationTestTargets.FLOAT, value);
    }

    static KeyframeAnimation held(String name,
                                  int blendInTicks,
                                  int durationTicks,
                                  int blendOutTicks,
                                  AnimationTarget<Float> target,
                                  float value) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(blendInTicks)
                .blendOutTicks(blendOutTicks)
                .tracks(List.of(AnimationTrack.<Float>builder()
                        .target(target)
                        .keyframes(List.of(new Keyframe<>(0, value, Easing.LINEAR)))
                        .build()))
                .build();
    }

    /**
     * Looping clip ramping {@code target} from zero to {@code peakValue} and back, so a sample reveals
     * where in its cycle the caller is.
     */
    static KeyframeAnimation ramping(String name, int durationTicks, AnimationTarget<Float> target, float peakValue) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(List.of(AnimationTrack.<Float>builder()
                        .target(target)
                        .keyframes(List.of(
                                new Keyframe<>(0, 0.0F, Easing.LINEAR),
                                new Keyframe<>(durationTicks / 2, peakValue, Easing.LINEAR),
                                new Keyframe<>(durationTicks, 0.0F, Easing.LINEAR)))
                        .build()))
                .build();
    }

    /**
     * Trackless looping clip that owns an arm configuration for its whole length.
     */
    static KeyframeAnimation armed(String name,
                                   int durationTicks,
                                   int blendOutTicks,
                                   ArmConfiguration armConfiguration) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(0)
                .blendOutTicks(blendOutTicks)
                .tracks(List.of())
                .armConfiguration(armConfiguration)
                .build();
    }

    static KeyframeAnimation empty(String name) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(1)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(List.of())
                .build();
    }

    private AnimationTestClips() {
    }

}
