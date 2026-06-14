package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public final class KeyframeAnimation {

    private final ResourceLocation id;
    private final int durationTicks;
    private final LoopMode loopMode;
    private final int blendInTicks;
    private final int blendOutTicks;
    private final List<AnimationTrack<?>> tracks;
    private final ArmConfigurationTimeline armConfigurationTimeline;

    public static TrackAnimationBuilder fromTracks() {
        return new TrackAnimationBuilder();
    }

    @Builder
    private KeyframeAnimation(@Nonnull ResourceLocation id,
                              int durationTicks,
                              @Nonnull LoopMode loopMode,
                              int blendInTicks,
                              int blendOutTicks,
                              @Nonnull List<AnimationTrack<?>> tracks,
                              @Nullable ArmConfiguration armConfiguration,
                              @Nullable ArmConfigurationTimeline armConfigurationTimeline,
                              @Nullable List<ArmConfigurationKeyframe> armConfigurationKeyframes) {
        if (durationTicks < 0) {
            throw new IllegalArgumentException("Animation duration must be non-negative");
        }
        if (blendInTicks < 0 || blendOutTicks < 0) {
            throw new IllegalArgumentException("Blend durations must be non-negative");
        }

        this.id = id;
        this.durationTicks = durationTicks;
        this.loopMode = loopMode;
        this.blendInTicks = blendInTicks;
        this.blendOutTicks = blendOutTicks;
        this.tracks = List.copyOf(tracks);
        this.armConfigurationTimeline = resolveArmConfigurationTimeline(
                armConfiguration,
                armConfigurationTimeline,
                armConfigurationKeyframes);
    }

    public Optional<ArmConfiguration> armConfigurationOverride() {
        return this.armConfigurationAt(0.0F);
    }

    public Optional<ArmConfiguration> armConfigurationAt(float elapsedTicks) {
        float animationTick = this.loopMode.resolveTick(elapsedTicks, this.durationTicks);
        return this.armConfigurationTimeline.sample(animationTick);
    }

    public AnimationFrame sample(float elapsedTicks) {
        if (this.tracks.isEmpty()) {
            return AnimationFrame.EMPTY;
        }

        float animationTick = this.loopMode.resolveTick(elapsedTicks, this.durationTicks);
        Map<AnimationTarget<?>, Object> values = new HashMap<>();
        for (AnimationTrack<?> track : this.tracks) {
            values.put(track.getTarget(), track.sample(animationTick));
        }
        return AnimationFrame.ofOwned(values);
    }

    private static ArmConfigurationTimeline resolveArmConfigurationTimeline(
            @Nullable ArmConfiguration armConfiguration,
            @Nullable ArmConfigurationTimeline armConfigurationTimeline,
            @Nullable List<ArmConfigurationKeyframe> armConfigurationKeyframes) {
        if (armConfigurationTimeline != null && armConfigurationKeyframes != null && !armConfigurationKeyframes.isEmpty()) {
            throw new IllegalArgumentException("Use either armConfigurationTimeline or armConfigurationKeyframes, not both");
        }
        if (armConfigurationTimeline != null) {
            return armConfigurationTimeline;
        }
        if (armConfigurationKeyframes != null && !armConfigurationKeyframes.isEmpty()) {
            return ArmConfigurationTimeline.of(armConfigurationKeyframes);
        }
        if (armConfiguration != null) {
            return ArmConfigurationTimeline.of(List.of(new ArmConfigurationKeyframe(0, armConfiguration)));
        }
        return ArmConfigurationTimeline.EMPTY;
    }

}
