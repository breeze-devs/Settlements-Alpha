package dev.breezes.settlements.domain.animation;

import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DefaultIdleLifeAnimatorFactory implements IdleLifeAnimatorFactory {

    private final IdleLifeAnimationLibrary idleLifeAnimationLibrary;

    @Override
    public IdleLifeAnimator create(int entityId) {
        return new DefaultIdleLifeAnimator(this.idleLifeAnimationLibrary, entityId);
    }

}
