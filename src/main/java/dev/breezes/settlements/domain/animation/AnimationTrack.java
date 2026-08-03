package dev.breezes.settlements.domain.animation;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

@Getter
public final class AnimationTrack<V> {

    private final AnimationTarget<V> target;
    private final List<Keyframe<V>> keyframes;

    @Builder
    private AnimationTrack(@Nonnull AnimationTarget<V> target,
                           @Nonnull List<Keyframe<V>> keyframes) {
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("AnimationTrack requires at least one keyframe");
        }

        List<Keyframe<V>> sortedKeyframes = keyframes.stream()
                .sorted(Comparator.comparingInt(Keyframe::tick))
                .toList();
        validateUniqueTicks(sortedKeyframes);

        this.target = target;
        this.keyframes = sortedKeyframes;
    }

    public V sample(float animationTick) {
        Keyframe<V> first = this.keyframes.getFirst();
        if (animationTick <= first.tick()) {
            return first.value();
        }

        // Negated rather than `animationTick > last.tick()` so a NaN tick lands on the held final value
        Keyframe<V> last = this.keyframes.getLast();
        if (!(animationTick <= last.tick())) {
            return last.value();
        }

        // Lowest index whose keyframe is at or past the sample. The bounds start at 1 because the sample
        // is already known to sit past the first keyframe, which guarantees a predecessor to blend from.
        int lowerIndex = 1;
        int upperIndex = this.keyframes.size() - 1;
        while (lowerIndex < upperIndex) {
            int middleIndex = (lowerIndex + upperIndex) >>> 1;
            if (animationTick <= this.keyframes.get(middleIndex).tick()) {
                upperIndex = middleIndex;
            } else {
                lowerIndex = middleIndex + 1;
            }
        }

        // The span needs no zero guard: ticks are sorted and validated unique at construction, so
        // consecutive keyframes are always at least one tick apart.
        Keyframe<V> current = this.keyframes.get(lowerIndex - 1);
        Keyframe<V> next = this.keyframes.get(lowerIndex);
        float normalized = (animationTick - current.tick()) / (next.tick() - current.tick());
        float eased = current.easingToNext().apply(normalized);
        return this.target.blend(current.value(), next.value(), eased);
    }

    private static <V> void validateUniqueTicks(@Nonnull List<Keyframe<V>> keyframes) {
        int previousTick = -1;
        for (Keyframe<V> keyframe : keyframes) {
            if (keyframe.tick() == previousTick) {
                throw new IllegalArgumentException("AnimationTrack keyframe ticks must be unique");
            }
            previousTick = keyframe.tick();
        }
    }

}
