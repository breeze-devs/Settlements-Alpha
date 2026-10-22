package dev.breezes.settlements.domain.farming;

import net.minecraft.resources.ResourceLocation;

public record CultivationCropDefinition(
        ResourceLocation seedItem,
        ResourceLocation displayItem,
        ResourceLocation cropBlock
) {
}
