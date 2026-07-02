package dev.breezes.settlements.domain.common.yields;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * Datapack codec for {@link WeightedYieldItem}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightedYieldItemCodec {

    public static final Codec<WeightedYieldItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("item").forGetter(WeightedYieldItem::item),
                    Codec.DOUBLE.fieldOf("weight").forGetter(WeightedYieldItem::weight),
                    Codec.INT.fieldOf("min_count").forGetter(WeightedYieldItem::minCount),
                    Codec.INT.fieldOf("max_count").forGetter(WeightedYieldItem::maxCount)
            ).apply(instance, WeightedYieldItem::new));

}
