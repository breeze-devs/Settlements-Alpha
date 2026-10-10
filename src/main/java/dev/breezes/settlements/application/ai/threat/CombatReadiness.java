package dev.breezes.settlements.application.ai.threat;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * One combat option's capability to fight, prepared for one villager, judging each hostile against it.
 */
public interface CombatReadiness {

    /**
     * Whether the option could engage the given hostile right now, were it selected.
     * <p>
     * Called only for a hostile already confirmed sighted, within the option's reach of the villager, and not a zombie
     * villager being cured.
     */
    boolean canEngage(@Nonnull LivingEntity hostile);

    /**
     * Whether the option can keep fighting a target it already engaged, perceiving it by its own means rather than
     * through the villager's awareness or sight.
     */
    default boolean canStayLockedOn(@Nonnull LivingEntity target) {
        return false;
    }

}
