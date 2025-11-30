package dev.breezes.settlements.models.behaviors.stages;

import dev.breezes.settlements.models.behaviors.StopBehaviorException;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.AbstractStep;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@CustomLog
public class StagedStep extends AbstractStep {

    private enum InternalStage implements StageKey {
        START,
        END;
    }

    private final Map<StageKey, BehaviorStep> stageStepMap;
    private final StageKey startingStage;
    private final StageKey initialActionStage;
    private final StageKey nextStage; // Stage to transition to after this StagedStep completes

    @Nullable
    private final BehaviorStep onStart;
    @Nullable
    private final BehaviorStep onEnd;

    @Getter
    private StageKey currentStage;

    @Builder
    private StagedStep(@Nonnull String name,
                       @Nonnull Map<StageKey, BehaviorStep> stageStepMap,
                       @Nonnull StageKey initialStage,
                       @Nonnull StageKey nextStage,
                       @Nullable BehaviorStep onStart,
                       @Nullable BehaviorStep onEnd) {
        super("StagedStep[%s]".formatted(name));
        if (stageStepMap.isEmpty()) {
            throw new IllegalArgumentException("Staged step must have at least one stage");
        }

        this.stageStepMap = new HashMap<>(stageStepMap);
        this.initialActionStage = initialStage;
        this.nextStage = nextStage;

        this.startingStage = InternalStage.START;
        // Map START to onStart wrapper
        this.stageStepMap.put(this.startingStage, this::onStart);

        this.currentStage = this.startingStage;

        this.onStart = onStart;
        this.onEnd = onEnd;
    }

    protected StepResult onStart(@Nonnull BehaviorContext context) {
        log.behaviorStatus("Starting {} ({})", this.getName(), this.getUuid());
        if (this.onStart != null) {
            this.onStart.tick(context);
        }
        return StepResult.transition(this.initialActionStage);
    }

    @Override
    public StepResult tick(@Nonnull BehaviorContext context) {
        BehaviorStep step = this.stageStepMap.get(this.currentStage);
        if (step == null) {
            log.error("Missing step for stage: {} in {} ({})", this.currentStage.name(), this.getName(), this.getUuid());
            return StepResult.abort("MISSING_STEP", new IllegalStateException("Missing step for stage: " + this.currentStage.name()));
        }

        StepResult result = step.tick(context);

        if (result instanceof StepResult.NoOp) {
            return StepResult.noOp();
        } else if (result instanceof StepResult.Transition(StageKey key)) {
            this.transitionStage(key);
            return StepResult.noOp();
        } else if (result instanceof StepResult.Complete) {
            // Inner step complete, treat as transition to END
            this.transitionStage(InternalStage.END);
            this.onEnd(context);
            // After onEnd, we are done with this StagedStep, so we transition to the *next* stage defined for the parent
            return StepResult.transition(this.nextStage);
        } else if (result instanceof StepResult.Fail fail) {
            // Bubble up the failure to the parent step
            return result;
        } else if (result instanceof StepResult.Abort) {
            return result;
        }

        return StepResult.noOp();
    }

    protected void onEnd(@Nonnull BehaviorContext context) {
        log.behaviorStatus("Ending {} ({})", this.getName(), this.getUuid());
        if (this.onEnd != null) {
            this.onEnd.tick(context);
        }
    }

    public void reset() {
        this.currentStage = this.startingStage;
    }

    private void transitionStage(@Nonnull StageKey stage) {
        if (stage == InternalStage.END) {
            log.behaviorStatus("End requested for {} ({})", this.getName(), this.getUuid());
            this.currentStage = InternalStage.END;
            return;
        }

        if (!this.stageStepMap.containsKey(stage)) {
            log.warn("Attempted to transition to an unknown stage: {} for {} ({})", stage.name(), this.getName(), this.getUuid());
            throw new StopBehaviorException("Attempted to transition to an unknown stage: %s".formatted(stage.name()));
        }

        log.behaviorStatus("Transitioning from {} to {} stage for {} ({})", this.currentStage.name(), stage.name(), this.getName(), this.getUuid());
        this.currentStage = stage;
    }

}
