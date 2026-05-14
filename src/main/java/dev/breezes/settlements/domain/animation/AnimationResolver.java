package dev.breezes.settlements.domain.animation;

import javax.annotation.Nonnull;

public interface AnimationResolver {

    KeyframeAnimation resolve(@Nonnull AnimationArchetype archetype, @Nonnull AnimationSelectionContext context);

}
