package dev.breezes.settlements.domain.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.entities.ExpertiseCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link EnchantmentCostData}.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnchantmentCostDataCodec {

    public static final Codec<EnchantmentCostData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("enchantment").forGetter(EnchantmentCostData::enchantmentId),
                    Codec.INT.fieldOf("base_cost").forGetter(EnchantmentCostData::baseCost),
                    Codec.INT.fieldOf("level_multiplier").forGetter(EnchantmentCostData::levelMultiplier),
                    Codec.INT.fieldOf("max_level").forGetter(EnchantmentCostData::maxLevel),
                    ExpertiseCodec.CODEC.fieldOf("min_tier").forGetter(EnchantmentCostData::minTier)
            ).apply(instance, EnchantmentCostData::new));

}
