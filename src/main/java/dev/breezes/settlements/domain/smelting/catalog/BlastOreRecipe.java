package dev.breezes.settlements.domain.smelting.catalog;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * A single blast-furnace ore recipe: {@code inputCount} of {@code input} produces {@code outputCount}
 * of {@code output}.
 */
@Builder
public record BlastOreRecipe(
        @Nonnull ResourceLocation input,
        @Nonnull ResourceLocation output,
        int inputCount,
        int outputCount
) {

    public BlastOreRecipe {
        if (inputCount < 1) {
            throw new IllegalArgumentException("Blast ore recipe input count must be at least 1");
        }
        if (outputCount < 1) {
            throw new IllegalArgumentException("Blast ore recipe output count must be at least 1");
        }
    }

}
