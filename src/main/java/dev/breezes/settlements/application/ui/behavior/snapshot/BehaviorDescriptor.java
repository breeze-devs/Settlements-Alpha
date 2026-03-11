package dev.breezes.settlements.application.ui.behavior.snapshot;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Builder
public record BehaviorDescriptor(
        @Nonnull String displayNameKey,
        @Nonnull ResourceLocation iconItemId,
        @Nullable String displaySuffix
) {

    public BehaviorDescriptor {
        Objects.requireNonNull(displayNameKey, "displayNameKey");
        Objects.requireNonNull(iconItemId, "iconItemId");
    }

}
