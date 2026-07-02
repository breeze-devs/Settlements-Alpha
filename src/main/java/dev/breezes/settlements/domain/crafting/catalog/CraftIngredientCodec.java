package dev.breezes.settlements.domain.crafting.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.economy.catalog.ItemMatchCodec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link CraftIngredient}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CraftIngredientCodec {

    public static final Codec<CraftIngredient> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ItemMatchCodec.CODEC.fieldOf("match").forGetter(CraftIngredient::match),
                    Codec.INT.fieldOf("count").forGetter(CraftIngredient::count)
            ).apply(instance, CraftIngredient::new));

}
