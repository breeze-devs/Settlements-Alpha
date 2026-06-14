package dev.breezes.settlements.domain.animation;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    private final TargetArithmetic<V> arithmetic;
    private final AnimationTargetPolicy policy;

    @Builder
    private AnimationTarget(@Nonnull String id,
                            @Nonnull Class<V> valueType,
                            @Nonnull V neutralValue,
                            @Nonnull Interpolator<V> interpolator,
                            @Nullable TargetArithmetic<V> arithmetic,
                            @Nonnull AnimationTargetPolicy policy) {
        this.id = id;
        this.valueType = valueType;
        this.neutralValue = neutralValue;
        this.interpolator = interpolator;
        this.arithmetic = arithmetic == null ? TargetArithmetics.forValueType(valueType) : arithmetic;
        this.policy = policy;
    }

    public V blend(@Nonnull V from, @Nonnull V to, float t) {
        return this.interpolator.interpolate(from, to, t);
    }

    public V compose(@Nonnull V base, @Nonnull V over, float weight) {
        float clampedWeight = Math.clamp(weight, 0.0F, 1.0F);
        if (clampedWeight <= 0.0F) {
            return base;
        }

        return switch (this.policy) {
            case ADDITIVE -> this.composeAdditive(base, over, clampedWeight);
            case MULTIPLICATIVE -> this.composeMultiplicative(base, over, clampedWeight);
            case ABSOLUTE -> this.blend(base, over, clampedWeight);
        };
    }

    private V composeAdditive(@Nonnull V base, @Nonnull V over, float weight) {
        V delta = this.arithmetic.subtract(over, this.neutralValue);
        return this.arithmetic.add(base, this.arithmetic.scale(delta, weight));
    }

    private V composeMultiplicative(@Nonnull V base, @Nonnull V over, float weight) {
        V factor = this.blend(this.neutralValue, over, weight);
        return this.arithmetic.multiply(base, factor);
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
