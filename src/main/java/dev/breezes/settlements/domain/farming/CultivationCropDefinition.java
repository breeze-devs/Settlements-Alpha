package dev.breezes.settlements.domain.farming;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public record CultivationCropDefinition(
        @Nonnull ResourceLocation seedItem,
        @Nonnull ResourceLocation displayItem,
        @Nonnull ResourceLocation cropBlock
) {
}
