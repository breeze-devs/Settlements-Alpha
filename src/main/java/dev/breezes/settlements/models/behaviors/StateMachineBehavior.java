package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.util.crash.CrashUtil;
import dev.breezes.settlements.util.crash.report.BehaviorConfigurationCrashReport;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class StateMachineBehavior extends AbstractBehavior<BaseVillager> {

    @Nullable
    private StagedStep controlStep;

    @Nullable
    private StageKey expectedEndStage;

    @Nullable
    private BehaviorContext context;

    protected StateMachineBehavior(@Nonnull ILogger log,
                                   @Nonnull ITickable preconditionCheckCooldown,
                                   @Nonnull ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
    }

    protected final void initializeStateMachine(@Nonnull StagedStep controlStep,
                                                @Nonnull StageKey expectedEndStage) {
        if (this.controlStep != null || this.expectedEndStage != null) {
            crashInvalidConfiguration("StateMachineBehavior for '%s' was initialized more than once"
                    .formatted(this.getClass().getSimpleName()));
        }
        this.controlStep = Objects.requireNonNull(controlStep);
        this.expectedEndStage = Objects.requireNonNull(expectedEndStage);
    }

    @Override
    public final void doStart(@Nonnull Level world,
                              @Nonnull BaseVillager entity) {
        this.requireInitialized();

        this.context = new BehaviorContext(entity);
        this.onBehaviorStart(world, entity, this.context);
        Objects.requireNonNull(this.controlStep).reset();
    }

    @Override
    public final void tickBehavior(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity) {
        this.requireInitialized();

        BehaviorContext context = this.requireContext();
        if (!this.preTickGuard(delta, world, entity, context)) {
            throw new StopBehaviorException("Behavior '%s' pre-tick guard failed".formatted(this.getClass().getSimpleName()));
        }

        StepResult result = Objects.requireNonNull(Objects.requireNonNull(this.controlStep).tick(context));
        this.handleStepResult(result, Objects.requireNonNull(this.expectedEndStage),
                Objects.requireNonNull(this.getClass().getSimpleName()));
    }

    @Override
    public final void doStop(@Nonnull Level world,
                             @Nonnull BaseVillager entity) {
        this.requireInitialized();

        this.onBehaviorStop(world, entity);
        this.context = null;
        Objects.requireNonNull(this.controlStep).reset();
    }

    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        // Empty by default, optionally overrideable by concrete behaviors
    }

    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager entity) {
        // Empty by default, optionally overrideable by concrete behaviors
    }

    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return true;
    }

    @Nonnull
    private BehaviorContext requireContext() {
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
                    throw new StopBehaviorException("Behavior '%s' has ended".formatted(behaviorName));
                }

                this.getLog().behaviorError("Behavior '{}' produced unexpected transition '{}' (expected end '{}')",
                        behaviorName, key.name(), endStage.name());
                throw new StopBehaviorException("Behavior '%s' unexpected transition '%s'".formatted(behaviorName, key.name()));
            }
            case StepResult.Fail fail -> {
                this.getLog().behaviorWarn("Behavior '{}' failed with code '{}' and details {}", behaviorName, fail.code(), fail.details());
                throw new StopBehaviorException("Behavior '%s' failed with code '%s'".formatted(behaviorName, fail.code()));
            }
            case StepResult.Abort abort -> {
                this.getLog().behaviorError("Behavior '{}' aborted with code '{}'", behaviorName, abort.code());
                if (abort.cause() != null) {
                    this.getLog().error(abort.cause(), "Behavior '{}' abort cause", behaviorName);
                }
                throw new StopBehaviorException("Behavior '%s' aborted with code '%s'".formatted(behaviorName, abort.code()));
            }
            default -> {
            }
        }
    }

}
