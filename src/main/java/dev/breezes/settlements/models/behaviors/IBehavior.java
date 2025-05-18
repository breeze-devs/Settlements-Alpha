package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.misc.ITickable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public interface IBehavior<T extends Entity> {

    /**
     * List of preconditions that must be met for this behavior to start
     */
    List<ICondition<T>> getPreconditions();

    /**
     * List of conditions that must be met for this behavior to continue running
     * - this can be completely different from preconditions, and the behavior will not check preconditions once running
     */
    List<ICondition<T>> getContinueConditions();

    /**
     * Cooldown for checking preconditions to prevent overloading the server
     * - note that this may delay the behavior beyond the behavior cooldown
     */
    ITickable getPreconditionCheckCooldown();

    /**
     * Cooldown for this behavior, can be set initially to delay the first run
     */
    ITickable getBehaviorCoolDown();

    BehaviorStatus getStatus();

    /**
     * Performs a check on all preconditions to determine if the behavior can start
     * <p>
     * This method will return false if the behavior is:
     * 1. already running
     * 2. the precondition check cooldown is still ticking
     *
     * @return whether the behavior should start
     */
    boolean tickPreconditions(int delta, @Nonnull Level world, @Nonnull T entity);

    /**
     * Logic to be performed when the behavior starts
     */
    void start(@Nonnull Level world, @Nonnull T entity);

    /**
     * Advance the behavior to the next delta ticks
     * <p>
     * This method does nothing if the behavior is not running
     */
    void tick(int delta, @Nonnull Level world, @Nonnull T entity);

    /**
     * Logic to be performed when the behavior stops
     */
    void stop(@Nonnull Level world, @Nonnull T entity);

    /**
     * Can be called any time to request the behavior to stop immediately
     * <p>
     * This is usually called within the behavior if a condition is no longer met
     */
    void requestStop();

}
