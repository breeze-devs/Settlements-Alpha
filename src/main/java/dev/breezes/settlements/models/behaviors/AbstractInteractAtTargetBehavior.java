package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.misc.ITickable;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public abstract class AbstractInteractAtTargetBehavior<T extends BaseVillager> extends AbstractBehavior<BaseVillager> {

    protected final ITickable interactionCooldown;

    public AbstractInteractAtTargetBehavior(@Nonnull ILogger log,
                                            @Nonnull ITickable preconditionCheckCooldown,
                                            @Nonnull ITickable behaviorCoolDown,
                                            @Nonnull ITickable interactionCooldown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        this.interactionCooldown = interactionCooldown;
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        // Check if we have a target
        if (!this.hasTarget(world, villager)) {
            this.requestStop();
            return;
        }

        if (this.isTargetInReach(world, villager)) {
            // Interact with the target with cooldown if we can reach it
            if (this.interactionCooldown.tickCheckAndReset(delta)) {
                this.interactWithTarget(delta, world, villager);
            }
        } else {
            // Navigate to the target if we can't reach it
            if (!villager.getNavigationManager().isNavigating()) {
                this.navigateToTarget(delta, world, villager);
            }
        }

        // Execute extra logic
        this.tickExtra(delta, world, villager);
    }

    /**
     * Navigates to the target
     * - only called by tick() when isTargetReachable() returns false
     */
    protected abstract void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager);

    /**
     * Interacts with the target
     * - only called by tick() when isTargetReachable() returns true
     */
    protected abstract void interactWithTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager);

    /**
     * Extra logic that can be handled per tick
     * - called by tick() regardless of the returns of isTargetReachable() after navigation/interaction
     */
    protected abstract void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager);

    /**
     * Determines whether this behavior has a valid target or not
     */
    protected abstract boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager);

    /**
     * Determines whether the target is within interaction range
     */
    protected abstract boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager);

}
