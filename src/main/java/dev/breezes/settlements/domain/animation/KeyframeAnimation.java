package dev.breezes.settlements.domain.animation;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class KeyframeAnimation {

    private final ResourceLocation id;
    private final int durationTicks;
    private final LoopMode loopMode;
    private final int blendInTicks;
    private final int blendOutTicks;
    private final List<AnimationTrack<?>> tracks;

    public static PoseAnimationBuilder fromPoses() {
        return new PoseAnimationBuilder();
    }

    @Builder
    private KeyframeAnimation(@Nonnull ResourceLocation id,
                              int durationTicks,
                              @Nonnull LoopMode loopMode,
                              int blendInTicks,
                              int blendOutTicks,
                              @Nonnull List<AnimationTrack<?>> tracks) {
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
        return AnimationFrame.of(values);
    }

}
