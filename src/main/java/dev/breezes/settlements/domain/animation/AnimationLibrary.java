package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;

import javax.annotation.Nonnull;

public interface AnimationLibrary {

    KeyframeAnimation resolve(@Nonnull AnimationArchetype archetype, @Nonnull ItemCategory category);

}
