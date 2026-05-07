package dev.breezes.settlements.application.ui.behavior.snapshot;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

@Builder
public record BehaviorUiDisplayInfo(
        @Nonnull String displayNameKey,
        @Nonnull ResourceLocation iconItemId
) {

}
