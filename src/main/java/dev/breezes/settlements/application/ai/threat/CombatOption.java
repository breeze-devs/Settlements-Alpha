package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * A combat capability a villager may fight with.
 */
public interface CombatOption {

    /**
     * This option's precedence among registered options, lowest first.
     * Unique among every option {@link CombatOptionCatalog} registers.
     */
    int order();

    /**
     * This option's contribution to the villager's willingness to fight when selected.
     */
    double nerveContribution();

    /**
     * The distance, in blocks, within which this option can engage a hostile.
     */
    double reachBlocks();

    /**
     * Whether this option could engage the given hostile right now, were it selected.
     * <p>
     * Called only for a hostile already confirmed sighted, within {@link #reachBlocks()}, and not a zombie villager
     * being cured.
     */
    boolean canEngage(@Nonnull BaseVillager villager, @Nonnull LivingEntity hostile);

    /**
     * Whether this option, as the villager's current selection, can keep fighting.
     * <p>
     * A cooldown between attacks or a reload in progress is not a loss of capability; only losing the
     * resource this option depends on is. Continuation is a property of the option itself, not of a
     * specific target: a hostile still in awareness is what the assessment separately requires.
     */
    boolean canContinue(@Nonnull BaseVillager villager);

    /**
     * Creates a fresh, unstarted combat action aimed at the given hostile.
     */
    IBehavior<BaseVillager> createBehavior(@Nonnull LivingEntity target);

}
