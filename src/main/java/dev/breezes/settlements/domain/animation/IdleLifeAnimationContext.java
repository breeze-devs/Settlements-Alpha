package dev.breezes.settlements.domain.animation;

public record IdleLifeAnimationContext(int entityId,
                                       long gameTime,
                                       float partialTicks,
                                       boolean actionActive) {
}
