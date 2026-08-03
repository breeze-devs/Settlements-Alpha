package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class AnimationFrame {

    public static final AnimationFrame EMPTY = new AnimationFrame(Map.of(), Map.of());

    private final Map<AnimationTarget<?>, Object> values;

    /**
     * Partial coverage for the coverage-tracked targets in {@link #values}, and the only place a frame
     * stores anything about coverage. Three rules keep it readable from a single lookup:
     * <ul>
     *     <li>every key here is also a key of {@link #values} — coverage without a value is meaningless;</li>
     *     <li>a present value with no entry here is fully covered, so the common case allocates nothing;</li>
     *     <li>a target covered by nothing at all is absent from {@link #values} too, never held at zero.</li>
     * </ul>
     * {@link #applicationWeight} is the only reader and {@link #putPartialWeight} the only writer.
     */
    private final Map<AnimationTarget<?>, Float> applicationWeights;

    /**
     * Whether any target in this frame is coverage-tracked, resolved once here because frames are
     * immutable and every fold would otherwise re-scan the whole target map to decide whether it needs
     * a coverage map at all.
     */
    private final boolean tracksCoverage;

    private AnimationFrame(@Nonnull Map<AnimationTarget<?>, Object> values,
                           @Nonnull Map<AnimationTarget<?>, Float> applicationWeights) {
        this.values = values;
        this.applicationWeights = applicationWeights;
        this.tracksCoverage = containsCoverageTrackedTarget(values);
    }

    public static AnimationFrame of(@Nonnull Map<AnimationTarget<?>, Object> values) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        // Defensive copy: the caller keeps ownership of its map and may mutate it afterward.
        return new AnimationFrame(Map.copyOf(values), Map.of());
    }

    /**
     * Ownership-transfer factory for internally built frames. The caller hands off a freshly created
     * map it neither retains nor mutates, so we skip the defensive copy that {@link #of} makes.
     * Per-frame compositing builds and discards several of these per villager, so the copy adds up.
     */
    static AnimationFrame ofOwned(@Nonnull Map<AnimationTarget<?>, Object> values) {
        return ofOwned(values, Map.of());
    }

    static AnimationFrame ofOwned(@Nonnull Map<AnimationTarget<?>, Object> values,
                                  @Nonnull Map<AnimationTarget<?>, Float> applicationWeights) {
        if (values.isEmpty()) {
            return EMPTY;
        }
        Map<AnimationTarget<?>, Float> retainedWeights = applicationWeights.isEmpty()
                ? Map.of()
                : applicationWeights;
        return new AnimationFrame(values, retainedWeights);
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
     * Returns how strongly an authored override covers the underlying pose. Present targets default
     * to full coverage; missing targets have no coverage.
     */
    public float applicationWeight(@Nonnull AnimationTarget<?> target) {
        if (!this.has(target)) {
            return 0.0F;
        }
        return this.applicationWeights.getOrDefault(target, 1.0F);
    }

    public AnimationFrame blendTo(@Nonnull AnimationFrame other, float t) {
        if (t <= 0.0F) {
            return this;
        }
        if (t >= 1.0F) {
            return other;
        }

        int maximumTargetCount = this.values.size() + other.values.size();
        Map<AnimationTarget<?>, Object> blended = HashMap.newHashMap(maximumTargetCount);
        // A crossfade turns full coverage on either side into partial coverage, so the presence of a
        // coverage-tracked target is enough to need the map even when neither input carries weights yet.
        Map<AnimationTarget<?>, Float> blendedWeights = this.tracksCoverage || other.tracksCoverage
                ? HashMap.newHashMap(maximumTargetCount)
                : Map.of();
        for (AnimationTarget<?> target : this.values.keySet()) {
            blendTargetInto(blended, blendedWeights, target, this, other, t);
        }
        for (AnimationTarget<?> target : other.values.keySet()) {
            if (!blended.containsKey(target)) {
                blendTargetInto(blended, blendedWeights, target, this, other, t);
            }
        }
        return AnimationFrame.ofOwned(blended, blendedWeights);
    }

    public AnimationFrame composeOver(@Nonnull AnimationFrame over, float weight) {
        if (over.values.isEmpty() || weight <= 0.0F) {
            return this;
        }
        if (this.values.isEmpty() && weight >= 1.0F) {
            return over;
        }

        Map<AnimationTarget<?>, Object> composed = HashMap.newHashMap(this.values.size() + over.values.size());
        composed.putAll(this.values);
        // Only the overlay's targets are folded, so this frame's own coverage matters solely where it is
        // already partial; full coverage on an untouched target survives as the absent-means-one default.
        Map<AnimationTarget<?>, Float> composedWeights;
        if (!this.applicationWeights.isEmpty() || over.tracksCoverage) {
            composedWeights = HashMap.newHashMap(this.applicationWeights.size() + over.values.size());
            composedWeights.putAll(this.applicationWeights);
        } else {
            composedWeights = Map.of();
        }
        for (AnimationTarget<?> target : over.values.keySet()) {
            composeTargetInto(composed, composedWeights, target, this, over, weight);
        }
        return AnimationFrame.ofOwned(composed, composedWeights);
    }

    private static <V> void blendTargetInto(@Nonnull Map<AnimationTarget<?>, Object> blended,
                                            @Nonnull Map<AnimationTarget<?>, Float> blendedWeights,
                                            @Nonnull AnimationTarget<V> target,
                                            @Nonnull AnimationFrame from,
                                            @Nonnull AnimationFrame to,
                                            float t) {
        if (target.getPolicy().isCoverageTracked()) {
            blendCoverageTrackedTargetInto(blended, blendedWeights, target, from, to, t);
            return;
        }
        // Missing tracks blend from a target-owned neutral so partial animations cannot leave stale poses behind.
        V fromValue = from.get(target);
        V toValue = to.get(target);
        blended.put(target, target.blend(fromValue, toValue, t));
    }

    private static <V> void blendCoverageTrackedTargetInto(@Nonnull Map<AnimationTarget<?>, Object> blended,
                                                           @Nonnull Map<AnimationTarget<?>, Float> blendedWeights,
                                                           @Nonnull AnimationTarget<V> target,
                                                           @Nonnull AnimationFrame from,
                                                           @Nonnull AnimationFrame to,
                                                           float t) {
        float fromContribution = (1.0F - t) * from.applicationWeight(target);
        float toContribution = t * to.applicationWeight(target);
        float outputWeight = Math.clamp(fromContribution + toContribution, 0.0F, 1.0F);
        if (outputWeight <= 0.0F) {
            return;
        }

        // Missing absolute targets represent absent coverage, not an authored neutral pose.
        V value;
        if (!from.has(target)) {
            value = to.get(target);
        } else if (!to.has(target)) {
            value = from.get(target);
        } else {
            value = target.blend(from.get(target), to.get(target), toContribution / outputWeight);
        }
        blended.put(target, value);
        putPartialWeight(blendedWeights, target, outputWeight);
    }

    private static <V> void composeTargetInto(@Nonnull Map<AnimationTarget<?>, Object> composed,
                                              @Nonnull Map<AnimationTarget<?>, Float> composedWeights,
                                              @Nonnull AnimationTarget<V> target,
                                              @Nonnull AnimationFrame base,
                                              @Nonnull AnimationFrame over,
                                              float weight) {
        if (target.getPolicy().isCoverageTracked()) {
            composeCoverageTrackedTargetInto(composed, composedWeights, target, base, over, weight);
            return;
        }
        V baseValue = target.getValueType().cast(composed.getOrDefault(target, target.getNeutralValue()));
        V overValue = over.get(target);
        composed.put(target, target.compose(baseValue, overValue, weight));
    }

    private static <V> void composeCoverageTrackedTargetInto(@Nonnull Map<AnimationTarget<?>, Object> composed,
                                                             @Nonnull Map<AnimationTarget<?>, Float> composedWeights,
                                                             @Nonnull AnimationTarget<V> target,
                                                             @Nonnull AnimationFrame base,
                                                             @Nonnull AnimationFrame over,
                                                             float weight) {
        float baseWeight = base.applicationWeight(target);
        float overWeight = over.applicationWeight(target) * Math.clamp(weight, 0.0F, 1.0F);
        float outputWeight = overWeight + baseWeight * (1.0F - overWeight);
        if (outputWeight <= 0.0F) {
            return;
        }

        // Normalize the authored values separately from coverage so applying the result once is
        // equivalent to applying the contributing override layers in order.
        V value = over.get(target);
        if (base.has(target)) {
            value = target.blend(base.get(target), value, overWeight / outputWeight);
        }
        composed.put(target, value);
        putPartialWeight(composedWeights, target, outputWeight);
    }

    /**
     * Records coverage below full, dropping the entry at full so the absent-means-one default carries
     * the common case. Callers must not store a zero here — a target nothing covers is left out of the
     * frame entirely instead.
     */
    private static void putPartialWeight(@Nonnull Map<AnimationTarget<?>, Float> weights,
                                         @Nonnull AnimationTarget<?> target,
                                         float weight) {
        if (weight >= 1.0F) {
            weights.remove(target);
            return;
        }
        weights.put(target, weight);
    }

    private static boolean containsCoverageTrackedTarget(@Nonnull Map<AnimationTarget<?>, Object> values) {
        for (AnimationTarget<?> target : values.keySet()) {
            if (target.getPolicy().isCoverageTracked()) {
                return true;
            }
        }
        return false;
    }

}
