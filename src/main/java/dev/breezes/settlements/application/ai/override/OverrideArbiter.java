package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.override.OverrideArbitrationRules;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Arbitrates override admission and preemption, coordinating execution with the day plan.
 * <p>
 * At most one override is installed per villager; only a strictly higher tier can replace it.
 */
@ServerScope
@CustomLog
public final class OverrideArbiter {

    private final List<OverridePolicy> orderedPolicies;
    private final IBehaviorCatalog catalog;
    private final DayPlanHandoff dayPlanHandoff;

    /**
     * Creates an arbiter with policies ordered by precedence.
     *
     * @throws IllegalArgumentException if two policies declare the same precedence
     */
    @Inject
    OverrideArbiter(@Nonnull Set<OverridePolicy> policies,
                    @Nonnull IBehaviorCatalog catalog,
                    @Nonnull DayPlanHandoff dayPlanHandoff) {
        List<OverridePolicy> ordered = policies.stream()
                .sorted(Comparator.comparing(OverridePolicy::precedence))
                .toList();
        Optional<OverridePrecedence> duplicate = OverrideArbitrationRules.firstDuplicate(
                ordered.stream().map(OverridePolicy::precedence).toList());
        Validate.isTrue(duplicate.isEmpty(), "Duplicate override precedence %s declared by more than one OverridePolicy", duplicate.orElse(null));

        this.orderedPolicies = ordered;
        this.catalog = catalog;
        this.dayPlanHandoff = dayPlanHandoff;
    }

    /**
     * Processes one server tick of override arbitration and execution. Policy evaluation is
     * throttled per villager.
     *
     * @return true when the override lane occupied the villager this tick, signaling the
     * caller to skip the day-plan tick
     */
    public boolean tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        OverrideRuntimeState runtime = villager.getOverrideRuntimeState();
        if (runtime.hasRunner()) {
            this.tickRunning(level, villager, runtime);
            return true;
        }

        return this.tryStart(level, villager, runtime);
    }

    /**
     * Synchronously stops and removes any installed override without re-queueing the interrupted
     * day-plan slot.
     */
    public void forceStop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        OverrideRuntimeState runtime = villager.getOverrideRuntimeState();
        OverrideRunner runner = runtime.getRunner();
        if (runner != null) {
            stopRunner(level, villager, runner);
        }
        runtime.clearRunner();
    }

    private void tickRunning(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRuntimeState runtime) {
        if (runtime.getEvaluationCooldown().tickCheckAndReset(1)) {
            if (!runtime.getOwningPolicy().isAdmissibleDuring(currentActivity(villager))) {
                this.stopAndRequeue(level, villager, runtime);
                return;
            }

            if (this.tryPreempt(level, villager, runtime)) {
                return;
            }
        }

        this.advanceRunner(level, villager, runtime);
    }

    /**
     * Attempts to replace the installed runner with an admissible, strictly higher-tier override.
     *
     * @return true once the outgoing runner has been removed, even if replacement startup fails;
     * false if no replacement passes validation
     */
    private boolean tryPreempt(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRuntimeState runtime) {
        OverrideTier runningTier = runtime.getRunner().tier();
        Activity activity = currentActivity(villager);
        Optional<Installed> candidate = this.prepareFirstRequestedOverride(level, villager,
                policy -> policy.precedence().tier().isAbove(runningTier) && policy.isAdmissibleDuring(activity));
        if (candidate.isEmpty()) {
            return false;
        }

        // Stop and tear down the outgoing runner before starting its replacement
        OverrideRunner outgoing = runtime.getRunner();
        stopRunner(level, villager, outgoing);
        runtime.clearRunner();

        Installed installed = candidate.get();
        if (!this.tryStartAndInstall(level, villager, runtime, installed)) {
            log.behaviorStatus("Override '{}' stopped for villager {}; replacement '{}' failed to start",
                    outgoing.diagnosticId(), villager.getUUID(), installed.runner().diagnosticId());
            this.dayPlanHandoff.reAttemptInterruptedSlot(villager);
            return true;
        }

        log.behaviorStatus("Override '{}' preempted '{}' for villager {}",
                installed.runner().diagnosticId(), outgoing.diagnosticId(), villager.getUUID());
        return true;
    }

    private void advanceRunner(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRuntimeState runtime) {
        OverrideRunner runner = runtime.getRunner();
        runner.tick(level, villager);
        if (runner.isComplete()) {
            this.stopAndRequeue(level, villager, runtime);
        }
    }

    private void stopAndRequeue(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRuntimeState runtime) {
        OverrideRunner runner = runtime.getRunner();
        String diagnosticId = runner.diagnosticId();
        stopRunner(level, villager, runner);
        runtime.clearRunner();
        this.dayPlanHandoff.reAttemptInterruptedSlot(villager);
        log.behaviorStatus("Override '{}' stopped for villager {}; re-queuing interrupted plan slot", diagnosticId, villager.getUUID());
    }

    private boolean tryStart(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRuntimeState runtime) {
        if (!runtime.getEvaluationCooldown().tickCheckAndReset(1)) {
            return false;
        }

        boolean planInterruptible = isPlanInterruptible(villager.getPlanRuntimeState().getCurrentDescriptor());
        Activity activity = currentActivity(villager);
        Optional<Installed> candidate = this.prepareFirstRequestedOverride(level, villager,
                policy -> OverrideArbitrationRules.isAdmittedDespiteProtectedPlan(policy.precedence().tier(), planInterruptible)
                        && policy.isAdmissibleDuring(activity));
        if (candidate.isEmpty()) {
            return false;
        }

        // Suspend before starting so teardown obligations are discharged before the override starts
        this.dayPlanHandoff.suspendIfActive(level, villager);
        this.dayPlanHandoff.reAttemptInterruptedSlot(villager);

        Installed installed = candidate.get();
        if (!this.tryStartAndInstall(level, villager, runtime, installed)) {
            return false;
        }

        log.behaviorStatus("Override '{}' installed for villager {}", installed.runner().diagnosticId(), villager.getUUID());
        return true;
    }

    /**
     * Installs a prepared override if it starts successfully. Requires an empty override slot.
     *
     * @return true if installed; false if startup throws a RuntimeException
     */
    private boolean tryStartAndInstall(@Nonnull ServerLevel level,
                                       @Nonnull BaseVillager villager,
                                       @Nonnull OverrideRuntimeState runtime,
                                       @Nonnull Installed installed) {
        try {
            installed.runner().start(level, villager);
        } catch (RuntimeException e) {
            log.behaviorError("Override '{}' threw during start for villager {}; skipping override",
                    installed.runner().diagnosticId(), villager.getUUID(), e);
            return false;
        }

        runtime.installRunner(installed.runner(), installed.policy());
        return true;
    }

    private static void stopRunner(@Nonnull ServerLevel level, @Nonnull BaseVillager villager, @Nonnull OverrideRunner runner) {
        try {
            runner.stop(level, villager);
        } catch (RuntimeException e) {
            log.behaviorError("Override '{}' threw during stop for villager {}; discarding it",
                    runner.diagnosticId(), villager.getUUID(), e);
        }
    }

    /**
     * Builds an unstarted runner for the first request among eligible policies, in precedence order.
     * Later policies are not evaluated once a request is found.
     *
     * @param eligible which policies may request an override
     * @return the runner and its policy, or empty if no request is found or the first request
     * names a catalog behavior that fails validation
     */
    private Optional<Installed> prepareFirstRequestedOverride(@Nonnull ServerLevel level,
                                                              @Nonnull BaseVillager villager,
                                                              @Nonnull Predicate<OverridePolicy> eligible) {
        // Filtered in place rather than through a stream, since this runs on every evaluation of every villager
        for (OverridePolicy policy : this.orderedPolicies) {
            if (!eligible.test(policy)) {
                continue;
            }

            Optional<OverrideRequest> request = policy.evaluate(level, villager);
            if (request.isEmpty()) {
                continue;
            }

            switch (request.get()) {
                case OverrideRequest.PreparedRunner prepared -> {
                    return Optional.of(new Installed(prepared.runner(), policy));
                }
                case OverrideRequest.CatalogBehavior catalogBehavior -> {
                    BehaviorKey key = catalogBehavior.behaviorKey();
                    OverrideTier tier = policy.precedence().tier();
                    // Validation failure must not hand the override to a request of lower precedence.
                    return this.validate(level, villager, key)
                            .map(behavior -> new Installed(
                                    new SimpleBehaviorOverrideRunner(key, behavior, tier, this.catalog), policy));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Builds and validates an unstarted behavior, bypassing its initial cooldowns.
     * TODO: we should find a better way to evaluate preconditions without spinning up an instance. This is a major overhaul that should be its own commit.
     *
     * @return the behavior, or empty if the key is unknown or its preconditions fail
     */
    private Optional<IBehavior<BaseVillager>> validate(@Nonnull ServerLevel level,
                                                       @Nonnull BaseVillager villager,
                                                       @Nonnull BehaviorKey key) {
        IBehavior<BaseVillager> behavior = this.catalog.createBehavior(key).orElse(null);
        if (behavior == null) {
            log.behaviorWarn("Override behavior '{}' not found in catalog for villager {}", key, villager.getUUID());
            return Optional.empty();
        }

        // Initial cooldowns must not delay consideration of a freshly created override behavior.
        behavior.getBehaviorCoolDown().forceComplete();
        behavior.getPreconditionCheckCooldown().forceComplete();

        if (!behavior.tickPreconditions(1, level, villager)) {
            log.behaviorTrace("Override preconditions not met for '{}' on villager {}", key, villager.getUUID());
            return Optional.empty();
        }

        return Optional.of(behavior);
    }

    @Nullable
    private static Activity currentActivity(@Nonnull BaseVillager villager) {
        return villager.getBrain().getActiveNonCoreActivity().orElse(null);
    }

    /**
     * Whether the descriptor permits interruption; missing metadata is treated as interruptible.
     */
    @VisibleForTesting
    static boolean isPlanInterruptible(@Nullable BehaviorPlanningMetadata descriptor) {
        // Treating missing metadata as protected would block ordinary overrides between plan slots.
        return descriptor == null || descriptor.isInterruptible();
    }

    private record Installed(OverrideRunner runner, OverridePolicy policy) {
    }

}
