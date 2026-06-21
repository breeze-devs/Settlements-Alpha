package dev.breezes.settlements.application.ai.behavior.runtime;

import dev.breezes.settlements.application.ai.behavior.teardown.ProvidesTeardownLedger;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look.LookQueries;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorDeedLedger;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.shared.logging.ILogger;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.BehaviorConfigurationCrashReport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public abstract class StateMachineBehavior<T extends Entity & ISettlementsBrainEntity> extends AbstractBehavior<T>
        implements ProducesBehaviorOutcome {

    @Nullable
    private StagedStep<T> controlStep;
    @Nullable
    private StageKey expectedEndStage;
    @Nullable
    private BehaviorContext<T> context;
    @Nullable
    private BehaviorDeedLedger lastDeeds;
    private BehaviorLifecycleResult lastLifecycleResult;

    private final ITickable lookControlCooldown;

    protected StateMachineBehavior(@Nonnull ILogger log,
                                   @Nonnull ITickable preconditionCheckCooldown,
                                   @Nonnull ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        this.lookControlCooldown = ClockTicks.of(10).asTickable();
        this.lastLifecycleResult = BehaviorLifecycleResult.clean();
    }

    protected final void initializeStateMachine(@Nonnull StagedStep<T> controlStep,
                                                @Nonnull StageKey expectedEndStage) {
        if (this.controlStep != null || this.expectedEndStage != null) {
            crashInvalidConfiguration("StateMachineBehavior for '%s' was initialized more than once"
                    .formatted(this.getClass().getSimpleName()));
        }
        this.controlStep = controlStep;
        this.expectedEndStage = expectedEndStage;
    }

    @Override
    public final void doStart(@Nonnull Level world,
                              @Nonnull T entity) {
        this.requireInitialized();

        this.context = new BehaviorContext<>(entity);
        this.lastDeeds = null;
        this.lastLifecycleResult = BehaviorLifecycleResult.clean();
        if (entity instanceof ProvidesTeardownLedger p) {
            this.context.getTeardownScope().bindLedger(p.getTeardownLedger());
        }
        this.onBehaviorStart(world, entity, this.context);
        this.controlStep.reset();
    }

    @Override
    public final void tickBehavior(int delta,
                                   @Nonnull Level world,
                                   @Nonnull T entity) {
        this.requireInitialized();

        BehaviorContext<T> context = this.requireContext();
        if (!this.preTickGuard(delta, world, entity, context)) {
            throw new StopBehaviorException("Behavior '%s' pre-tick guard failed".formatted(this.getClass().getSimpleName()));
        }

        StepResult result = this.controlStep.tick(context);
        if (this.lookControlCooldown.tickCheckAndReset(1) && this.shouldLookAtActiveTarget()) {
            LookQueries.resolveLookLocation(context).ifPresent(entity::lookAt);
        }
        this.handleStepResult(result, this.expectedEndStage, this.getClass().getSimpleName());
    }

    @Override
    public final void doStop(@Nonnull Level world,
                             @Nonnull T entity) {
        this.requireInitialized();

        try {
            this.onBehaviorStop(world, entity);
        } finally {
            if (this.context != null) {
                this.lastDeeds = this.context.getState(BehaviorStateType.BEHAVIOR_OUTCOME, BehaviorDeedLedger.class)
                        .orElse(null);
                this.context.getTeardownScope().teardownAll(this.context.getLevel());
            }
            this.context = null;
            this.controlStep.reset();
        }
    }

    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull T entity,
                                   @Nonnull BehaviorContext<T> context) {
        // Empty by default, optionally overrideable by concrete behaviors
    }

    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull T entity) {
        // Empty by default, optionally overrideable by concrete behaviors
    }

    protected Optional<String> getCurrentStageLabel() {
        if (this.controlStep == null || this.controlStep.getCurrentStage() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.controlStep.getCurrentStage().name());
    }

    /**
     * Reads a state from the live behavior context, or {@link Optional#empty()} when no run is
     * active. Intended for terminal hooks such as {@link #onBehaviorStop} that must inspect the
     * run's accumulated outcome before the context is torn down.
     */
    protected final <S extends BehaviorState> Optional<S> getContextState(@Nonnull BehaviorStateType type,
                                                                          @Nonnull Class<S> castTo) {
        if (this.context == null) {
            return Optional.empty();
        }
        return this.context.getState(type, castTo);
    }

    @Override
    public Optional<BehaviorDeedLedger> getLastDeeds() {
        return Optional.ofNullable(this.lastDeeds);
    }

    @Nonnull
    @Override
    public BehaviorLifecycleResult getLastLifecycleResult() {
        return this.lastLifecycleResult;
    }

    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull T entity,
                                   @Nonnull BehaviorContext<T> context) {
        return true;
    }

    /**
     * Whether the framework should automatically resolve and apply a gaze direction each tick.
     * The resolved direction is the explicit {@code LookState} if the behavior set one, otherwise
     * the navigation target — so most behaviors get correct look-at-target behavior for free.
     * Override to {@code false} only when the behavior manages two-party gaze itself (e.g. a trade
     * interaction driven by {@code EntityTracker}) and must not have any auto-look applied.
     */
    protected boolean shouldLookAtActiveTarget() {
        return true;
    }

    private BehaviorContext<T> requireContext() {
        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }
        return this.context;
    }

    private void requireInitialized() {
        if (this.controlStep == null || this.expectedEndStage == null) {
            crashInvalidConfiguration("StateMachineBehavior for '%s' was not initialized. ".formatted(this.getClass().getSimpleName())
                    + "Call initializeStateMachine(...) as the final step of subclass constructor.");
        }
    }

    private static void crashInvalidConfiguration(@Nonnull String message) throws IllegalArgumentException {
        CrashUtil.crash(new BehaviorConfigurationCrashReport(new IllegalArgumentException(message)));
    }

    /**
     * Standardized handling for staged-step result bubbling.
     */
    protected final void handleStepResult(@Nonnull StepResult result,
                                          @Nonnull StageKey endStage,
                                          @Nonnull String behaviorName) {
        if (result instanceof StepResult.NoOp || result instanceof StepResult.Complete) {
            return;
        }

        switch (result) {
            case StepResult.Transition(StageKey key) -> {
                if (key.equals(endStage)) {
                    this.lastLifecycleResult = BehaviorLifecycleResult.clean();
                    throw new StopBehaviorException("Behavior '%s' has ended".formatted(behaviorName));
                }

                this.getLog().behaviorError("Behavior '{}' produced unexpected transition '{}' (expected end '{}')",
                        behaviorName, key.name(), endStage.name());
                this.lastLifecycleResult = BehaviorLifecycleResult.fail("unexpected transition '%s'".formatted(key.name()));
                throw new StopBehaviorException("Behavior '%s' unexpected transition '%s'".formatted(behaviorName, key.name()));
            }
            case StepResult.Fail fail -> {
                this.getLog().behaviorWarn("Behavior '{}' failed with code '{}' and details {}", behaviorName, fail.code(), fail.details());
                this.lastLifecycleResult = BehaviorLifecycleResult.fail(fail.code());
                throw new StopBehaviorException("Behavior '%s' failed with code '%s'".formatted(behaviorName, fail.code()));
            }
            case StepResult.Abort abort -> {
                this.getLog().behaviorError("Behavior '{}' aborted with code '{}'", behaviorName, abort.code());
                if (abort.cause() != null) {
                    this.getLog().error(abort.cause(), "Behavior '{}' abort cause", behaviorName);
                }
                this.lastLifecycleResult = BehaviorLifecycleResult.abort(abort.code());
                throw new StopBehaviorException("Behavior '%s' aborted with code '%s'".formatted(behaviorName, abort.code()));
            }
            default -> {
            }
        }
    }

}
