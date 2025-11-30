package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.util.Ticks;
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
public class TimeBasedStep extends AbstractStep {

    private final ITickable tickable;
    /**
     * Map of ticks elapsed to the behavior step to execute
     */
    private final Map<Long, BehaviorStep> keyFrames;

    /**
     * Map of intervals to the behavior steps to execute periodically
     */
    @Nonnull
    private final Map<Integer, List<BehaviorStep>> periodicSteps;

    @Nullable
    private final BehaviorStep onStart;
    @Nullable
    private final BehaviorStep onEnd;

    public TimeBasedStep(@Nonnull ITickable tickable,
                         @Nullable BehaviorStep onStart,
                         @Nullable BehaviorStep onEnd) {
        super("TimeBasedStep[%s]".formatted(tickable.getRemainingCooldownsAsPrettyString()));
        this.tickable = tickable;
        this.keyFrames = new HashMap<>();
        this.periodicSteps = new HashMap<>();

        this.onStart = onStart;
        this.onEnd = onEnd;
    }

    public void addKeyFrames(@Nonnull Map<Long, BehaviorStep> keyFrames) {
        this.keyFrames.putAll(keyFrames);
    }

    public void addPeriodicSteps(@Nonnull Map<Integer, List<BehaviorStep>> periodicSteps) {
        this.periodicSteps.putAll(periodicSteps);
    }

    @Override
    public StepResult tick(@Nonnull BehaviorContext context) {
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
        for (Map.Entry<Integer, List<BehaviorStep>> entry : this.periodicSteps.entrySet()) {
            int interval = entry.getKey();
            if (elapsed % interval == 0) {
                for (BehaviorStep step : entry.getValue()) {
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

        BehaviorStep step = this.keyFrames.get(elapsed);
        return step.tick(context);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable
        private ITickable tickable;
        @Nonnull
        private final Map<Long, BehaviorStep> keyFrames;
        @Nonnull
        private final Map<Integer, List<BehaviorStep>> periodicSteps;
        @Nullable
        private BehaviorStep onStart;
        @Nullable
        private BehaviorStep onEnd;

        public Builder() {
            this.keyFrames = new HashMap<>();
            this.periodicSteps = new HashMap<>();
        }

        public Builder withTickable(@Nonnull ITickable tickable) {
            this.tickable = tickable;
            return this;
        }

        public Builder addKeyFrame(@Nonnull Ticks elapsed, @Nonnull BehaviorStep step) {
            this.keyFrames.put(elapsed.getTicks(), step);
            return this;
        }

        public Builder everyTick(@Nonnull BehaviorStep step) {
            return this.addPeriodicStep(1, step);
        }

        public Builder addPeriodicStep(int interval, @Nonnull BehaviorStep step) {
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be greater than 0");
            }
            List<BehaviorStep> steps = this.periodicSteps.getOrDefault(interval, new ArrayList<>());
            steps.add(step);
            this.periodicSteps.put(interval, steps);
            return this;
        }

        public Builder onStart(@Nonnull BehaviorStep step) {
            this.onStart = step;
            return this;
        }

        public Builder onEnd(@Nonnull BehaviorStep step) {
            this.onEnd = step;
            return this;
        }

        public TimeBasedStep build() {
            if (this.tickable == null) {
                throw new IllegalStateException("Tickable must be set");
            }
            TimeBasedStep step = new TimeBasedStep(this.tickable, this.onStart, this.onEnd);
            step.addKeyFrames(this.keyFrames);
            step.addPeriodicSteps(this.periodicSteps);
            return step;
        }

    }

}
