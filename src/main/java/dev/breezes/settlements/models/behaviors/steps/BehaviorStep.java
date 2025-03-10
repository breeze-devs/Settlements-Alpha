package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Defines the contract for a step in a behavior
 */
public interface BehaviorStep {

    Optional<Stage> tick(@Nonnull BehaviorContext context);

//    boolean canContinue(@Nonnull BehaviorContext context

}
