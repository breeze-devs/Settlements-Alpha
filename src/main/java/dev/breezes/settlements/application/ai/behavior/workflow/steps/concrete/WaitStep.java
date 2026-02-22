package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.time.Tickable;
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
    public StepResult tick(@Nonnull BehaviorContext context) {
        if (waitTime.tickCheckAndReset(1)) {
            return StepResult.transition(nextStage);
        }
        return StepResult.noOp();
    }

    @Override
    public void reset() {
        this.waitTime.reset();
    }

}
