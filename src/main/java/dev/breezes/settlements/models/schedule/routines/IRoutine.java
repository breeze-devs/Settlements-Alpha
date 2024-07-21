package dev.breezes.settlements.models.schedule.routines;

import dev.breezes.settlements.models.behaviors.IBehavior;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Represents a "routine" such as rest, work, etc
 */
public interface IRoutine<T extends Entity> {

    /**
     * Get the list of behaviors that make up this routine
     * <p>
     * For example, work routine may contain "shear sheep" behavior, "plant crops" behavior, etc.
     */
    List<IBehavior<T>> getBehaviors();

}
