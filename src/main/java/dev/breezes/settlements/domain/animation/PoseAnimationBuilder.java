package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PoseAnimationBuilder {

    private ResourceLocation id;
    private int durationTicks;
    private LoopMode loopMode = LoopMode.ONCE;
    private int blendInTicks;
    private int blendOutTicks;
    private ArmConfiguration armConfiguration = ArmConfiguration.BOTH_CROSSED;
    private final List<PoseKeyframe> poseKeyframes = new ArrayList<>();
    private final List<AnimationTrack<?>> manualTracks = new ArrayList<>();

    public PoseAnimationBuilder id(@Nonnull ResourceLocation id) {
        this.id = id;
        return this;
    }

    public PoseAnimationBuilder durationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
        return this;
    }

    public PoseAnimationBuilder loopMode(@Nonnull LoopMode loopMode) {
        this.loopMode = loopMode;
        return this;
    }

    public PoseAnimationBuilder blendInTicks(int blendInTicks) {
        this.blendInTicks = blendInTicks;
        return this;
    }

    public PoseAnimationBuilder blendOutTicks(int blendOutTicks) {
        this.blendOutTicks = blendOutTicks;
        return this;
    }

    public PoseAnimationBuilder arms(@Nonnull ArmConfiguration armConfiguration) {
        this.armConfiguration = armConfiguration;
        return this;
    }

    /**
     * Adds a pose sample to the timeline. Each target is keyed independently: if an intermediate pose omits
     * a target, that target interpolates directly between the surrounding poses that do include it.
     */
    public PoseAnimationBuilder at(int tick, @Nonnull Pose pose, @Nonnull Easing easingToNext) {
        this.poseKeyframes.add(new PoseKeyframe(tick, pose, easingToNext));
        return this;
    }

    public PoseAnimationBuilder track(@Nonnull AnimationTrack<?> track) {
        this.manualTracks.add(track);
        return this;
    }

    public KeyframeAnimation build() {
        List<AnimationTrack<?>> tracks = new ArrayList<>(this.poseTracks());
        tracks.addAll(this.manualTracks);

        return KeyframeAnimation.builder()
                .id(this.id)
                .durationTicks(this.durationTicks)
                .loopMode(this.loopMode)
                .blendInTicks(this.blendInTicks)
                .blendOutTicks(this.blendOutTicks)
                .tracks(tracks)
                .armConfiguration(this.armConfiguration)
                .build();
    }

    private List<AnimationTrack<?>> poseTracks() {
        Set<AnimationTarget<?>> manualTargets = new HashSet<>();
        for (AnimationTrack<?> manualTrack : this.manualTracks) {
            manualTargets.add(manualTrack.getTarget());
        }

        Map<AnimationTarget<?>, List<Keyframe<?>>> keyframesByTarget = new HashMap<>();
        for (PoseKeyframe poseKeyframe : this.poseKeyframes) {
            for (AnimationTarget<?> target : poseKeyframe.pose().targets()) {
                if (!manualTargets.contains(target)) {
                    appendKeyframe(keyframesByTarget, target, poseKeyframe);
                }
            }
        }

        List<AnimationTrack<?>> tracks = new ArrayList<>();
        for (Map.Entry<AnimationTarget<?>, List<Keyframe<?>>> entry : keyframesByTarget.entrySet()) {
            tracks.add(track(entry.getKey(), entry.getValue()));
        }
        return tracks;
    }

    private static <V> void appendKeyframe(@Nonnull Map<AnimationTarget<?>, List<Keyframe<?>>> keyframesByTarget,
                                           @Nonnull AnimationTarget<V> target,
                                           @Nonnull PoseKeyframe poseKeyframe) {
        keyframesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>())
                .add(new Keyframe<>(poseKeyframe.tick(), poseKeyframe.pose().require(target), poseKeyframe.easingToNext()));
    }

    @SuppressWarnings("unchecked")
    private static <V> AnimationTrack<V> track(@Nonnull AnimationTarget<?> target,
                                               @Nonnull List<Keyframe<?>> keyframes) {
        // Pose construction type-checks every value against its target, so regrouping by target preserves V.
        return AnimationTrack.<V>builder()
                .target((AnimationTarget<V>) target)
                .keyframes((List<Keyframe<V>>) (List<?>) keyframes)
                .build();
    }

    private record PoseKeyframe(int tick, @Nonnull Pose pose, @Nonnull Easing easingToNext) {
    }

}
