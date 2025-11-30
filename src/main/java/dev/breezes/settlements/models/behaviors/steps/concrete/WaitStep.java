package dev.breezes.settlements.models.behaviors.steps.concrete;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.AbstractStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.misc.Tickable;
import lombok.Builder;

import javax.annotation.Nonnull;

/**
 * A step that waits for a fixed amount of time before transitioning to the next stage
 */
public class WaitStep extends AbstractStep {

    private final Tickable waitTime;
    private final StageKey nextStage;

    @Builder
    public WaitStep(@Nonnull Tickable waitTime, @Nonnull StageKey nextStage) {
        super("WaitStep[%s]".formatted(waitTime.getRemainingCooldownsAsPrettyString()));
        this.waitTime = waitTime;
        this.nextStage = nextStage;
    }

    @Override
    public StepResult tick(BehaviorContext context) {
        if (waitTime.tickCheckAndReset(1)) {
            return StepResult.transition(nextStage);
        }
        return StepResult.noOp();
    }

}
