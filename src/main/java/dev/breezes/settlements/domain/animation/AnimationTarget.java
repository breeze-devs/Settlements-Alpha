package dev.breezes.settlements.domain.animation;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Objects;

@Getter
public final class AnimationTarget<V> {

    /**
     * Globally unique target identity. Equality intentionally uses only this id so separately resolved
     * references to the same target still address the same sampled frame value.
     */
    private final String id;
    private final Class<V> valueType;
    private final V neutralValue;
    private final Interpolator<V> interpolator;
    private final AnimationTargetPolicy policy;

    @Builder
    private AnimationTarget(@Nonnull String id,
                            @Nonnull Class<V> valueType,
                            @Nonnull V neutralValue,
                            @Nonnull Interpolator<V> interpolator,
                            @Nonnull AnimationTargetPolicy policy) {
        this.id = id;
        this.valueType = valueType;
        this.neutralValue = neutralValue;
        this.interpolator = interpolator;
        this.policy = policy;
    }

    public V blend(@Nonnull V from, @Nonnull V to, float t) {
        return this.interpolator.interpolate(from, to, t);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AnimationTarget<?> other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

}
