package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class TrackAnimationBuilder {

    private ResourceLocation id;
    private int durationTicks;
    private LoopMode loopMode = LoopMode.ONCE;
    private int blendInTicks;
    private int blendOutTicks;
    private ArmConfiguration armConfiguration = ArmConfiguration.BOTH_CROSSED;
    private final List<ArmConfigurationKeyframe> armConfigurationKeyframes = new ArrayList<>();
    private final List<AnimationTrack<?>> tracks = new ArrayList<>();

    public TrackAnimationBuilder id(@Nonnull ResourceLocation id) {
        this.id = id;
        return this;
    }

    public TrackAnimationBuilder durationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
        return this;
    }

    public TrackAnimationBuilder loopMode(@Nonnull LoopMode loopMode) {
        this.loopMode = loopMode;
        return this;
    }

    public TrackAnimationBuilder blendInTicks(int blendInTicks) {
        this.blendInTicks = blendInTicks;
        return this;
    }

    public TrackAnimationBuilder blendOutTicks(int blendOutTicks) {
        this.blendOutTicks = blendOutTicks;
        return this;
    }

    public TrackAnimationBuilder arms(@Nonnull ArmConfiguration armConfiguration) {
        this.armConfiguration = armConfiguration;
        return this;
    }

    public TrackAnimationBuilder armConfigurationAt(int tick, @Nonnull ArmConfiguration armConfiguration) {
        this.armConfigurationKeyframes.add(new ArmConfigurationKeyframe(tick, armConfiguration));
        return this;
    }

    public TrackAnimationBuilder track(@Nonnull AnimationTrack<?> track) {
        this.tracks.add(track);
        return this;
    }

    public KeyframeAnimation build() {
        return KeyframeAnimation.builder()
                .id(this.id)
                .durationTicks(this.durationTicks)
                .loopMode(this.loopMode)
                .blendInTicks(this.blendInTicks)
                .blendOutTicks(this.blendOutTicks)
                .tracks(this.tracks)
                .armConfigurationKeyframes(this.armConfigurationTimeline())
                .build();
    }

    private List<ArmConfigurationKeyframe> armConfigurationTimeline() {
        List<ArmConfigurationKeyframe> timeline = new ArrayList<>();
        timeline.add(new ArmConfigurationKeyframe(0, this.armConfiguration));
        timeline.addAll(this.armConfigurationKeyframes);
        return timeline;
    }

}
