package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a series of behavior steps based on the elapsed ticks of a {@link ITickable}
 * <p>
 * If a keyframe is exactly the start or end of the tickable, it will not be executed.
 * Use the onStart and onEnd steps for that.
 */
@CustomLog
public class TimeBasedStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private final ITickable tickable;
    /**
     * Map of ticks elapsed to the behavior step to execute
     */
    private final Map<Long, BehaviorStep<T>> keyFrames;

    /**
     * Map of intervals to the behavior steps to execute periodically
     */
    @Nonnull
    private final Map<Integer, List<BehaviorStep<T>>> periodicSteps;

    @Nullable
    private final BehaviorStep<T> onStart;
    @Nullable
    private final BehaviorStep<T> onEnd;

    public TimeBasedStep(@Nonnull ITickable tickable,
                         @Nullable BehaviorStep<T> onStart,
                         @Nullable BehaviorStep<T> onEnd) {
        super("TimeBasedStep[%s]".formatted(tickable.getRemainingCooldownsAsPrettyString()));
        this.tickable = tickable;
        this.keyFrames = new HashMap<>();
        this.periodicSteps = new HashMap<>();

        this.onStart = onStart;
        this.onEnd = onEnd;
    }

    public void addKeyFrames(@Nonnull Map<Long, BehaviorStep<T>> keyFrames) {
        this.keyFrames.putAll(keyFrames);
    }

    public void addPeriodicSteps(@Nonnull Map<Integer, List<BehaviorStep<T>>> periodicSteps) {
        this.periodicSteps.putAll(periodicSteps);
    }

    @Override
    public StepResult tick(@Nonnull BehaviorContext<T> context) {
        if (this.onStart != null && this.tickable.getTicksElapsed() == 0) {
            StepResult result = this.onStart.tick(context);
            if (!(result instanceof StepResult.NoOp)) {
                return result;
            }
        }

        boolean completed = this.tickable.tickCheckAndReset(1);
        if (completed) {
            // By default, we return complete when timer runs out
            if (this.onEnd == null) {
                log.behaviorStatus("No onEnd step, returning complete");
                return StepResult.complete();
            }

            // This return can be overridden by the onEnd step
            StepResult nextStage = this.onEnd.tick(context);
            log.behaviorStatus("Next stage: {}", nextStage);

            // If onEnd returns NoOp, we still want to signal completion
            if (nextStage instanceof StepResult.NoOp) {
                return StepResult.complete();
            }
            return nextStage;
        }

        // Execute periodic steps
        long elapsed = this.tickable.getTicksElapsedRounded();
        for (Map.Entry<Integer, List<BehaviorStep<T>>> entry : this.periodicSteps.entrySet()) {
            int interval = entry.getKey();
            if (elapsed % interval == 0) {
                for (BehaviorStep<T> step : entry.getValue()) {
                    StepResult result = step.tick(context);
                    // Break out of the execution if any result is not NoOp
                    if (!(result instanceof StepResult.NoOp)) {
                        return result;
                    }
                }
            }
        }

        // Execute keyframes
        if (!this.keyFrames.containsKey(elapsed)) {
            return StepResult.noOp();
        }

        BehaviorStep<T> step = this.keyFrames.get(elapsed);
        return step.tick(context);
    }

    @Override
    public void reset() {
        this.tickable.reset();

        if (this.onStart != null) {
            this.onStart.reset();
        }
        if (this.onEnd != null) {
            this.onEnd.reset();
        }

        this.periodicSteps.values().forEach(steps -> steps.forEach(BehaviorStep::reset));
        this.keyFrames.values().forEach(BehaviorStep::reset);
    }

    public static <T extends ISettlementsBrainEntity> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends ISettlementsBrainEntity> {

        @Nullable
        private ITickable tickable;
        @Nonnull
        private final Map<Long, BehaviorStep<T>> keyFrames;
        @Nonnull
        private final Map<Integer, List<BehaviorStep<T>>> periodicSteps;
        @Nullable
        private BehaviorStep<T> onStart;
        @Nullable
        private BehaviorStep<T> onEnd;

        public Builder() {
            this.keyFrames = new HashMap<>();
            this.periodicSteps = new HashMap<>();
        }

        public Builder<T> withTickable(@Nonnull ITickable tickable) {
            this.tickable = tickable;
            return this;
        }

        public Builder<T> addKeyFrame(@Nonnull ClockTicks elapsed, @Nonnull BehaviorStep<T> step) {
            this.keyFrames.put(elapsed.getTicks(), step);
            return this;
        }

        public Builder<T> everyTick(@Nonnull BehaviorStep<T> step) {
            return this.addPeriodicStep(1, step);
        }

        public Builder<T> addPeriodicStep(int interval, @Nonnull BehaviorStep<T> step) {
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be greater than 0");
            }
            List<BehaviorStep<T>> steps = this.periodicSteps.getOrDefault(interval, new ArrayList<>());
            steps.add(step);
            this.periodicSteps.put(interval, steps);
            return this;
        }

        public Builder<T> onStart(@Nonnull BehaviorStep<T> step) {
            this.onStart = step;
            return this;
        }

        public Builder<T> onEnd(@Nonnull BehaviorStep<T> step) {
            this.onEnd = step;
            return this;
        }

        public TimeBasedStep<T> build() {
            if (this.tickable == null) {
                throw new IllegalStateException("Tickable must be set");
            }
            TimeBasedStep<T> step = new TimeBasedStep<>(this.tickable, this.onStart, this.onEnd);
            step.addKeyFrames(this.keyFrames);
            step.addPeriodicSteps(this.periodicSteps);
            return step;
        }

    }

}
