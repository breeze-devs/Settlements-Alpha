package dev.breezes.settlements.domain.common.yields;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link WeightedYieldTable}. {@code selection_weight} defaults to {@code 1.0}
 * when a file omits it, matching every current "default" fallback file.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightedYieldTableCodec {

    public static final Codec<WeightedYieldTable> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("block").forGetter(WeightedYieldTable::block),
                    Codec.DOUBLE.optionalFieldOf("selection_weight", 1.0D).forGetter(WeightedYieldTable::selectionWeight),
                    Codec.unboundedMap(Codec.STRING, WeightedYieldPoolCodec.CODEC).fieldOf("expertise_pools").forGetter(WeightedYieldTable::pools)
            ).apply(instance, WeightedYieldTable::new));

}
