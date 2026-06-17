package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * A single override trigger strategy evaluated by {@link dev.breezes.settlements.application.ai.planning.PlanRunner}
 * on every tick.
 * <p>
 * Implementations must be stateless and pure: they read world and villager state but must
 * never mutate either. Side-effects (registry updates, behavior configuration) belong in the
 * override launcher inside {@code PlanRunner}, not here.
 * <p>
 * Return an empty Optional when the policy has no trigger to fire.
 * Return a present Optional when the policy identifies an override that should be installed.
 */
public interface OverridePolicy {

    /**
     * Cross-policy precedence for override selection. Larger values win.
     */
    int priority();

    /**
     * Evaluates whether this policy wants to fire an override for the given villager.
     *
     * @param level    current server level
     * @param villager the villager being ticked
     * @return a present {@link OverrideRequest} if an override should be installed, or empty
     */
    Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

}
