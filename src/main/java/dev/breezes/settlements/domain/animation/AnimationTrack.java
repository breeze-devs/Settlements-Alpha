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

        for (int i = 0; i < this.keyframes.size() - 1; i++) {
            Keyframe<V> current = this.keyframes.get(i);
            Keyframe<V> next = this.keyframes.get(i + 1);
            if (animationTick <= next.tick()) {
                float span = next.tick() - current.tick();
                float normalized = span <= 0.0F ? 1.0F : (animationTick - current.tick()) / span;
                float eased = current.easingToNext().apply(normalized);
                return this.target.blend(current.value(), next.value(), eased);
            }
        }

        return this.keyframes.getLast().value();
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
