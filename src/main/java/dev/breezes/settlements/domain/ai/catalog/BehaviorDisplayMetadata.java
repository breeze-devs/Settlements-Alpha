package dev.breezes.settlements.domain.ai.catalog;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

@Builder
public record BehaviorDisplayMetadata(
        @Nonnull String displayNameKey,
        @Nonnull ResourceLocation iconItemId
) {

}
