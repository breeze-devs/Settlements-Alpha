package dev.breezes.settlements.domain.animation;

import lombok.Builder;
import lombok.Getter;

/**
 * Bundles the animator collaborators a {@link LayerStack} is constructed with.
 * <p>
 * Every slot defaults to its no-op constant, leaving a caller to name only the animators it supplies.
 */
@Getter
@Builder
public final class LayerStackAnimators {

    @Builder.Default
    private final IdleLifeAnimator idleLife = IdleLifeAnimator.NONE;

    @Builder.Default
    private final LocomotionAnimator locomotion = LocomotionAnimator.NONE;

    @Builder.Default
    private final UmbrellaAnimator umbrella = UmbrellaAnimator.NONE;

}
