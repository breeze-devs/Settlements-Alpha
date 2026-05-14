package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Pose {

    public static final Pose EMPTY = new Pose(Map.of());

    private final Map<AnimationTarget<?>, Object> valuesByTarget;

    private Pose(@Nonnull Map<AnimationTarget<?>, Object> valuesByTarget) {
        this.valuesByTarget = Map.copyOf(valuesByTarget);
    }

    public static <V> Pose of(@Nonnull AnimationTarget<V> target, @Nonnull V value) {
        return EMPTY.with(target, value);
    }

    public <V> Pose with(@Nonnull AnimationTarget<V> target, @Nonnull V value) {
        Map<AnimationTarget<?>, Object> mergedValues = new HashMap<>(this.valuesByTarget);
        // The target owns its value type, so validate at pose construction instead of failing while sampling.
        mergedValues.put(target, target.getValueType().cast(value));
        return new Pose(mergedValues);
    }

    public Pose with(@Nonnull Pose overlay) {
        if (overlay.valuesByTarget.isEmpty()) {
            return this;
        }

        Map<AnimationTarget<?>, Object> mergedValues = new HashMap<>(this.valuesByTarget);
        mergedValues.putAll(overlay.valuesByTarget);
        return new Pose(mergedValues);
    }

    public Set<AnimationTarget<?>> targets() {
        return this.valuesByTarget.keySet();
    }

    public <V> Optional<V> find(@Nonnull AnimationTarget<V> target) {
        Object value = this.valuesByTarget.get(target);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(target.getValueType().cast(value));
    }

    <V> V require(@Nonnull AnimationTarget<V> target) {
        return this.find(target)
                .orElseThrow(() -> new IllegalArgumentException("Pose does not contain target: " + target.getId()));
    }

}
