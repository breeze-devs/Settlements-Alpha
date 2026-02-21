package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.logging.ILogger;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.misc.ITickable;

import javax.annotation.Nonnull;

public abstract class BaseVillagerStagedBehavior extends AbstractBehavior<BaseVillager> {

    protected BaseVillagerStagedBehavior(@Nonnull ILogger log, @Nonnull ITickable preconditionCheckCooldown, @Nonnull ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
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
