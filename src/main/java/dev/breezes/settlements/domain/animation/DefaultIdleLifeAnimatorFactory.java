package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DefaultIdleLifeAnimatorFactory implements IdleLifeAnimatorFactory {

    private final IdleLifeAnimationLibrary idleLifeAnimationLibrary;

    @Override
    public IdleLifeAnimator create(int entityId) {
        return new DefaultIdleLifeAnimator(this.idleLifeAnimationLibrary, entityId);
    }

}
