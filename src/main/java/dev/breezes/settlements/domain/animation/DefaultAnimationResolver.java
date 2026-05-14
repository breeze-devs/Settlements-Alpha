package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DefaultAnimationResolver implements AnimationResolver {

    private final AnimationLibrary animationLibrary;

    @Override
    public KeyframeAnimation resolve(@Nonnull AnimationArchetype archetype, @Nonnull AnimationSelectionContext context) {
        return this.animationLibrary.resolve(archetype, context.mainHandCategory());
    }

}
