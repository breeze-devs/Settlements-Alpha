package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class AnimationFrame {

    public static final AnimationFrame EMPTY = new AnimationFrame(Map.of());

    private final Map<AnimationTarget<?>, Object> values;

    private AnimationFrame(@Nonnull Map<AnimationTarget<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static AnimationFrame of(@Nonnull Map<AnimationTarget<?>, Object> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        return new AnimationFrame(values);
    }

    public <V> V get(@Nonnull AnimationTarget<V> target) {
        return this.get(target, target.getNeutralValue());
    }

    public <V> V get(@Nonnull AnimationTarget<V> target, @Nonnull V fallback) {
        Object value = this.values.get(target);
        if (value == null) {
            return fallback;
        }
        return target.getValueType().cast(value);
    }

    public boolean has(@Nonnull AnimationTarget<?> target) {
        return this.values.containsKey(target);
    }

    /**
     * Splice {@code overrides} on top of this frame. Untouched targets keep their existing values;
     * targets present in {@code overrides} replace whatever this frame had. Unlike {@link #blendTo},
     * targets absent from {@code overrides} are not blended toward neutral.
     */
    public AnimationFrame overlay(@Nonnull Map<AnimationTarget<?>, Object> overrides) {
        if (overrides.isEmpty()) {
            return this;
        }
        Map<AnimationTarget<?>, Object> merged = new HashMap<>(this.values);
        merged.putAll(overrides);
        return AnimationFrame.of(merged);
    }

    public AnimationFrame blendTo(@Nonnull AnimationFrame other, float t) {
        Map<AnimationTarget<?>, Object> blended = new HashMap<>();
        this.values.keySet().forEach(target -> blendTargetInto(blended, target, this, other, t));
        other.values.keySet().stream()
                .filter(target -> !blended.containsKey(target))
                .forEach(target -> blendTargetInto(blended, target, this, other, t));
        return AnimationFrame.of(blended);
    }

    private static <V> void blendTargetInto(@Nonnull Map<AnimationTarget<?>, Object> blended,
                                            @Nonnull AnimationTarget<V> target,
                                            @Nonnull AnimationFrame from,
                                            @Nonnull AnimationFrame to,
                                            float t) {
        // Missing tracks blend from a target-owned neutral so partial animations cannot leave stale poses behind.
        V fromValue = from.get(target);
        V toValue = to.get(target);
        blended.put(target, target.blend(fromValue, toValue, t));
    }

}
