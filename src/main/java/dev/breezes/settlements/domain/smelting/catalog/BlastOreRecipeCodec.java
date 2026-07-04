package dev.breezes.settlements.domain.smelting.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

/**
 * Datapack codec for {@link BlastOreRecipe}. Counts default to 1 so the common 1:1 ore recipes
 * can omit them entirely.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlastOreRecipeCodec {

    public static final Codec<BlastOreRecipe> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("input").forGetter(BlastOreRecipe::input),
                    ResourceLocation.CODEC.fieldOf("output").forGetter(BlastOreRecipe::output),
                    Codec.INT.optionalFieldOf("input_count", 1).forGetter(BlastOreRecipe::inputCount),
                    Codec.INT.optionalFieldOf("output_count", 1).forGetter(BlastOreRecipe::outputCount)
            ).apply(instance, BlastOreRecipe::new));

}
