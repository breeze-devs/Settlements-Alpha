package dev.breezes.settlements.models.schedule.routines;

import dev.breezes.settlements.models.behaviors.IBehavior;
import net.minecraft.world.entity.Entity;

/**
 * Provides behavior for a routine
 */
public interface IRoutineBehaviorProvider<T extends Entity> {

    IBehavior<T> provideBehavior();

}
