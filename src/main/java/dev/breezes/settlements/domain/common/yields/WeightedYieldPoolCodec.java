package dev.breezes.settlements.domain.common.yields;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link WeightedYieldPool}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightedYieldPoolCodec {

    public static final Codec<WeightedYieldPool> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("rolls").forGetter(WeightedYieldPool::rolls),
                    WeightedYieldItemCodec.CODEC.listOf().fieldOf("items").forGetter(WeightedYieldPool::items)
            ).apply(instance, WeightedYieldPool::new));

}
