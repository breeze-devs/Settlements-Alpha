package dev.breezes.settlements.domain.ai.catalog;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

@Builder
public record BehaviorDisplayMetadata(
        String displayNameKey,
        ResourceLocation iconItemId
) {

}
