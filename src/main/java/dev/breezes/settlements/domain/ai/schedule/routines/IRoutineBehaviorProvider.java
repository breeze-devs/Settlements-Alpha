package dev.breezes.settlements.domain.ai.schedule.routines;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import net.minecraft.world.entity.Entity;

/**
 * Provides behavior for a routine
 */
public interface IRoutineBehaviorProvider<T extends Entity> {

    IBehavior<T> provideBehavior();

}
