package dev.breezes.settlements.application.ai.behavior.publication;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorLifecycleResult;
import dev.breezes.settlements.application.ai.behavior.runtime.ProducesBehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorDeedLedger;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Single publication seam for terminal behavior outcomes.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class BehaviorOutcomePublisher {

    private final WorldEventEmitter worldEventEmitter;

    public void publishCompleted(@Nonnull BaseVillager villager,
                                 @Nonnull BehaviorKey key,
                                 @Nonnull IBehavior<BaseVillager> behavior) {
        if (!(behavior instanceof ProducesBehaviorOutcome outcomeProducer)) {
            this.worldEventEmitter.emitBehaviorCompleted(villager, key);
            return;
        }

        BehaviorLifecycleResult lifecycleResult = outcomeProducer.getLastLifecycleResult();
        BehaviorDeedLedger ledger = outcomeProducer.getLastDeeds().orElse(null);
        if (ledger == null) {
            this.publishWithoutOutcome(villager, key, lifecycleResult);
            return;
        }

        BehaviorOutcome primary = ledger.primary().orElse(null);
        if (primary == null) {
            this.publishWithoutOutcome(villager, key, lifecycleResult);
            return;
        }

        // A silent outcome owns its own memory (or deliberately leaves none); publish nothing.
        if (primary.isSilent()) {
            return;
        }

        if (!lifecycleResult.isClean() && !primary.hasExplicitEventOutcome()) {
            this.worldEventEmitter.emitBehaviorFailed(villager, key, lifecycleResult.getReason());
            return;
        }

        boolean emittedAny = false;
        for (BehaviorOutcome outcome : ledger.entriesView()) {
            if (outcome.isSilent() || !outcome.hasDeclaredDeed()) {
                continue;
            }
            this.publishDeed(villager, key, outcome);
            emittedAny = true;
        }

        if (emittedAny) {
            return;
        }

        this.publishWithoutOutcome(villager, key, lifecycleResult);
    }

    /**
     * Publishes a terminal failure for a behavior that was force-stopped externally rather than
     * failing through its own lifecycle (e.g. the plan runner's run-duration ceiling). The caller
     * supplies the reason because the behavior itself holds no record of why it was stopped.
     */
    public void publishFailed(@Nonnull BaseVillager villager,
                              @Nonnull BehaviorKey key,
                              @Nullable String reason) {
        this.worldEventEmitter.emitBehaviorFailed(villager, key, reason);
    }

    private void publishWithoutOutcome(@Nonnull BaseVillager villager,
                                       @Nonnull BehaviorKey key,
                                       @Nonnull BehaviorLifecycleResult lifecycleResult) {
        if (lifecycleResult.isClean()) {
            this.worldEventEmitter.emitBehaviorCompleted(villager, key);
        } else {
            this.worldEventEmitter.emitBehaviorFailed(villager, key, lifecycleResult.getReason());
        }
    }

    private void publishDeed(@Nonnull BaseVillager villager,
                             @Nonnull BehaviorKey key,
                             @Nonnull BehaviorOutcome outcome) {
        WorldEventType deedType = outcome.getDeedType();
        if (deedType == null) {
            this.worldEventEmitter.emitBehaviorCompleted(villager, key);
            return;
        }

        EventOutcome eventOutcome = outcome.getEventOutcome();
        if (eventOutcome == null) {
            eventOutcome = outcome.isSuccess() ? EventOutcome.SUCCESS : null;
        }

        this.worldEventEmitter.emitTerminalBehaviorEvent(villager, key, deedType,
                outcome.getPartnerId(), outcome.getRegistryId(), eventOutcome,
                outcome.resolveDetail(), resolveReason(outcome));
    }

    @Nullable
    private static String resolveReason(@Nonnull BehaviorOutcome outcome) {
        if (outcome.getEventOutcome() == EventOutcome.FAILURE) {
            return outcome.getFailureReason();
        }

        return null;
    }

}
