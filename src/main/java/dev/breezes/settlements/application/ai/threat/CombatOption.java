package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A combat capability a villager may fight with.
 */
public interface CombatOption {

    /**
     * This option's contribution to the villager's willingness to fight when selected.
     */
    double nerveContribution();

    /**
     * The farthest, in blocks, a hostile can be from the villager for this option to engage it or stay locked on to it.
     * An option that fights from somewhere else, such as a machine, covers the walk there as well.
     */
    double reachBlocks();

    /**
     * Whether the villager can fight with this option, and if so, what answers for each hostile; null when it cannot.
     * <p>
     * A cooldown between attacks or a reload in progress is not a loss of capability.
     * Readiness is a property of the villager and the option, not of a specific target.
     */
    @Nullable
    CombatReadiness prepare(@Nonnull BaseVillager villager);

    /**
     * Creates a fresh, unstarted combat action aimed at the given hostile.
     */
    IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target);

    /**
     * The name and icon a player sees while one of this option's combat actions runs.
     */
    BehaviorDisplayMetadata displayInfo();

}
