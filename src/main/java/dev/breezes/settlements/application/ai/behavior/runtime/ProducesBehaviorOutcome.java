package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;

import java.util.Optional;

public interface ProducesBehaviorOutcome {

    Optional<BehaviorOutcome> getLastOutcome();

    BehaviorLifecycleResult getLastLifecycleResult();

}
