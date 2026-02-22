package dev.breezes.settlements.application.ai.behavior.workflow.staged;

import dev.breezes.settlements.application.ai.behavior.runtime.StopBehaviorException;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.BehaviorConfigurationCrashReport;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@CustomLog
public class StagedStep extends AbstractStep {

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
            crashInvalidConfiguration("Staged step must have at least one stage");
        }

        this.stageStepMap = new HashMap<>(stageStepMap);
        this.validate(this.stageStepMap, onStart, onEnd);

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
            StepResult result = this.onStart.tick(context);
            if (!(result instanceof StepResult.NoOp)) {
                return result;
            }
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
            // Inner step completes, treat as transition to END
            this.transitionStage(InternalStage.END);
            this.onEnd(context);
            // After onEnd, we are done with this StagedStep, so we transition to the *next* stage defined for the parent
            return StepResult.transition(this.nextStage);
        } else if (result instanceof StepResult.Fail) {
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

        if (this.onStart != null) {
            this.onStart.reset();
        }
        if (this.onEnd != null) {
            this.onEnd.reset();
        }

        for (BehaviorStep step : this.stageStepMap.values()) {
            step.reset();
        }
    }

    private void validate(@Nonnull Map<StageKey, BehaviorStep> stageStepMap,
                          @Nullable BehaviorStep onStart,
                          @Nullable BehaviorStep onEnd) {
        Map<BehaviorStep, StageKey> seenByIdentity = new IdentityHashMap<>();
        for (Map.Entry<StageKey, BehaviorStep> entry : stageStepMap.entrySet()) {
            StageKey stage = entry.getKey();
            BehaviorStep step = entry.getValue();
            if (step == null) {
                crashInvalidConfiguration("Stage '%s' has a null step".formatted(stage.name()));
            }

            StageKey previousStage = seenByIdentity.put(step, stage);
            if (previousStage != null) {
                crashInvalidConfiguration("Each stage must have a unique step instance. Duplicate instance used by stages '%s' and '%s'"
                        .formatted(previousStage.name(), stage.name()));
            }
        }

        if (onStart != null && seenByIdentity.containsKey(onStart)) {
            crashInvalidConfiguration("onStart step instance must be unique and not reused by stage '%s'"
                    .formatted(seenByIdentity.get(onStart).name()));
        }
        if (onEnd != null && seenByIdentity.containsKey(onEnd)) {
            crashInvalidConfiguration("onEnd step instance must be unique and not reused by stage '%s'"
                    .formatted(seenByIdentity.get(onEnd).name()));
        }
        if (onStart != null && onStart == onEnd) {
            crashInvalidConfiguration("onStart and onEnd must be different step instances");
        }
    }

    private static void crashInvalidConfiguration(@Nonnull String message) throws IllegalArgumentException {
        CrashUtil.crash(new BehaviorConfigurationCrashReport(new IllegalArgumentException(message)));
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

    private enum InternalStage implements StageKey {
        START,
        END;
    }

}
