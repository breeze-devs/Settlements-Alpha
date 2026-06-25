package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.inject.Inject;

/**
 * Bundles the cross-cutting and broadly-shared dependencies that many behaviors share
 */
@Getter
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class BehaviorSupport {

    private final HungerConfig hungerConfig;
    private final WorldEventEmitter worldEventEmitter;
    private final DemandSignalService demandSignalService;
    private final DemandEvaluator demandEvaluator;
    private final SupplyEvaluator supplyEvaluator;
    private final BlockMemoryTargetResolver blockMemoryTargetResolver;

}
