package dev.breezes.settlements.domain.animation;

/**
 * Minecraft-free time slice plus the one domain-relevant shouldDeploy gate.
 */
public record UmbrellaAnimationContext(int entityId,
                                       long gameTime,
                                       float partialTicks,
                                       boolean shouldDeploy) {
}
