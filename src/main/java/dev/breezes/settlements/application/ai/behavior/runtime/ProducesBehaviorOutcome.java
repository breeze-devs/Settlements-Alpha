package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorDeedLedger;

import java.util.Optional;

public interface ProducesBehaviorOutcome {

    Optional<BehaviorDeedLedger> getLastDeeds();

    BehaviorLifecycleResult getLastLifecycleResult();

}
