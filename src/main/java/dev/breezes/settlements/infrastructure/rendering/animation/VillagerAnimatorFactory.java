package dev.breezes.settlements.infrastructure.rendering.animation;

import dev.breezes.settlements.di.ClientSessionScope;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.IdleLifeAnimatorFactory;
import dev.breezes.settlements.domain.animation.LayerStackAnimators;
import dev.breezes.settlements.domain.animation.LocomotionAnimator;
import dev.breezes.settlements.domain.animation.UmbrellaAnimatorFactory;
import dev.breezes.settlements.domain.animation.VillagerAnimator;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Builds a villager's client animator from the session's animation collaborators.
 */
@ClientSessionScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerAnimatorFactory {

    private final AnimationResolver animationResolver;
    private final IdleLifeAnimatorFactory idleLifeAnimatorFactory;
    private final LocomotionAnimator locomotionAnimator;
    private final UmbrellaAnimatorFactory umbrellaAnimatorFactory;

    public VillagerAnimator create(int entityId) {
        return new VillagerAnimator(this.animationResolver, LayerStackAnimators.builder()
                .idleLife(this.idleLifeAnimatorFactory.create(entityId))
                .locomotion(this.locomotionAnimator)
                .umbrella(this.umbrellaAnimatorFactory.create())
                .build(), entityId);
    }

}
