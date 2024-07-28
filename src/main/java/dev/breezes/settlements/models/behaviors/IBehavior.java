package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.misc.ITickable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public interface IBehavior<T extends Entity> {

    /**
     * List of preconditions that must be met for this behavior to start
     */
    List<IEntityCondition<T>> getPreconditions();

    /**
     * List of conditions that must be met for this behavior to continue running
     * - this can be completely different from preconditions, and the behavior will not check preconditions once running
     */
    List<IEntityCondition<T>> getContinueConditions();

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
     * Perform the behavior or tick cooldowns
     * <p>
     * Should be called every delta ticks
     */
    void tick(int delta, @Nonnull Level world, @Nonnull T entity);

    /**
     * Logic to be performed when the behavior starts
     */
    void start(@Nonnull Level world, @Nonnull T entity);

    /**
     * Can be called any time to request the behavior to stop immediately
     * <p>
     * This is called within the behavior if a condition is no longer met
     */
    void requestStop();

    /**
     * Logic to be performed when the behavior stops
     */
    void stop(@Nonnull Level world, @Nonnull T entity);

}
