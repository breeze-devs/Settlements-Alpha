package dev.breezes.settlements.domain.crafting.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Datapack codec for {@link CraftRecipe}. {@code priority} defaults to 0.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CraftRecipeCodec {

    public static final Codec<CraftRecipe> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(CraftRecipe::id),
                    CraftIngredientCodec.CODEC.listOf().fieldOf("inputs").forGetter(CraftRecipe::inputs),
                    CraftOutputCodec.CODEC.fieldOf("output").forGetter(CraftRecipe::output),
                    Codec.INT.optionalFieldOf("priority", 0).forGetter(CraftRecipe::priority)
            ).apply(instance, CraftRecipe::new));

}
