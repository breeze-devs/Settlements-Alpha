package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;

import javax.annotation.Nonnull;

public record AnimationKey(@Nonnull AnimationArchetype archetype,
                           @Nonnull ItemCategory category) {

    public static AnimationKey of(@Nonnull AnimationArchetype archetype, @Nonnull ItemCategory category) {
        return new AnimationKey(archetype, category);
    }

}
