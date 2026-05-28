package dev.breezes.settlements.application.ai.behavior.workflow.staged;

import dev.breezes.settlements.application.ai.behavior.runtime.StopBehaviorException;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
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
public class StagedStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private final Map<StageKey, BehaviorStep<T>> stageStepMap;
    private final StageKey startingStage;
    private final StageKey initialActionStage;
    private final StageKey nextStage; // Stage to transition to after this StagedStep completes

    @Nullable
    private final BehaviorStep<T> onStart;
    @Nullable
    private final BehaviorStep<T> onEnd;

    @Getter
    private StageKey currentStage;

    @Builder
    private StagedStep(@Nonnull String name,
                       @Nonnull Map<StageKey, BehaviorStep<T>> stageStepMap,
                       @Nonnull StageKey initialStage,
                       @Nonnull StageKey nextStage,
                       @Nullable BehaviorStep<T> onStart,
                       @Nullable BehaviorStep<T> onEnd) {
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

    protected StepResult onStart(@Nonnull BehaviorContext<T> context) {
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
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        BehaviorStep<T> step = this.stageStepMap.get(this.currentStage);
        if (step == null) {
            log.error("Missing step for stage: {} in {} ({})", this.currentStage.name(), this.getName(), this.getUuid());
            return StepResult.abort("MISSING_STEP", new IllegalStateException("Missing step for stage: " + this.currentStage.name()));
        }

        StepResult result = step.tick(context);

        if (result instanceof StepResult.NoOp) {
            return StepResult.noOp();
        } else if (result instanceof StepResult.Transition(StageKey key)) {
            // When an inner step explicitly transitions to the outer StagedStep's own exit stage
            // (e.g. AWARD → Stage.END), treat it as clean completion rather than calling
            // transitionStage(key), which would fail with "unknown stage" because the
            // behavior-level terminal stage is intentionally absent from the stageStepMap.
            if (key.equals(this.nextStage)) {
                this.transitionStage(InternalStage.END);
                this.onEnd(context);
                return StepResult.transition(this.nextStage);
            }
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

    protected void onEnd(@Nonnull BehaviorContext<T> context) {
        log.behaviorStatus("Ending {} ({})", this.getName(), this.getUuid());
        if (this.onEnd != null) {
            this.onEnd.tick(context);
        }
    }

    @Override
    protected void doOnEnter() {
        // Per-entry reset: restart the state machine at the START stage. Stages themselves
        // get per-entry reset via transitionStage() as the machine advances — no eager cascade
        // here, because eager cascade would clear cross-entry state (loop counters) inside
        // stages we haven't entered yet on this re-entry.
        this.currentStage = this.startingStage;
    }

    @Override
    protected void doReset() {
        // Per-run reset: full cascade. Each stage step gets its full reset() so nested
        // cross-entry state (LoopBackStep.remaining etc.) clears between behavior runs.
        if (this.onStart != null) {
            this.onStart.reset();
        }
        if (this.onEnd != null) {
            this.onEnd.reset();
        }

        for (BehaviorStep<T> step : this.stageStepMap.values()) {
            step.reset();
        }
    }

    private void validate(@Nonnull Map<StageKey, BehaviorStep<T>> stageStepMap,
                          @Nullable BehaviorStep<T> onStart,
                          @Nullable BehaviorStep<T> onEnd) {
        Map<BehaviorStep<T>, StageKey> seenByIdentity = new IdentityHashMap<>();
        for (Map.Entry<StageKey, BehaviorStep<T>> entry : stageStepMap.entrySet()) {
            StageKey stage = entry.getKey();
            BehaviorStep<T> step = entry.getValue();
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

        BehaviorStep<T> nextStep = this.stageStepMap.get(stage);
        if (nextStep == null) {
            log.warn("Attempted to transition to an unknown stage: {} for {} ({})", stage.name(), this.getName(), this.getUuid());
            throw new StopBehaviorException("Attempted to transition to an unknown stage: %s".formatted(stage.name()));
        }

        log.behaviorStatus("Transitioning from {} to {} stage for {} ({})", this.currentStage.name(), stage.name(), this.getName(), this.getUuid());
        this.currentStage = stage;
        // Per-entry lifecycle: give the destination step a fresh start so timeouts, tickables, and
        // nested sequence/state indices don't carry over from a previous entry on this same run.
        nextStep.onEnter();
    }

    private enum InternalStage implements StageKey {
        START,
        END;
    }

}
