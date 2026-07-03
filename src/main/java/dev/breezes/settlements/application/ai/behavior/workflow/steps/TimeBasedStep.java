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
     * Map of periodic schedules (interval + phase offset) to the behavior steps to execute on that cadence
     */
    @Nonnull
    private final Map<PeriodicSchedule, List<BehaviorStep<T>>> periodicSteps;

    @Nullable
    private final BehaviorStep<T> onStart;
    @Nullable
    private final BehaviorStep<T> onEnd;

    public TimeBasedStep(@Nonnull String name,
                         @Nonnull ITickable tickable,
                         @Nullable BehaviorStep<T> onStart,
                         @Nullable BehaviorStep<T> onEnd) {
        super(name);
        this.tickable = tickable;
        this.keyFrames = new HashMap<>();
        this.periodicSteps = new HashMap<>();

        this.onStart = onStart;
        this.onEnd = onEnd;
    }

    public void addKeyFrames(@Nonnull Map<Long, BehaviorStep<T>> keyFrames) {
        this.keyFrames.putAll(keyFrames);
    }

    public void addPeriodicSteps(@Nonnull Map<PeriodicSchedule, List<BehaviorStep<T>>> periodicSteps) {
        this.periodicSteps.putAll(periodicSteps);
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
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
        for (Map.Entry<PeriodicSchedule, List<BehaviorStep<T>>> entry : this.periodicSteps.entrySet()) {
            PeriodicSchedule schedule = entry.getKey();
            if (elapsed % schedule.interval() == schedule.offset()) {
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
    protected void doOnEnter() {
        // Per-entry: rewind the tickable so the keyframes / onStart / onEnd fire correctly on
        // re-entry within a run, and propagate the per-entry reset to substeps.
        this.tickable.reset();

        if (this.onStart != null) {
            this.onStart.onEnter();
        }
        if (this.onEnd != null) {
            this.onEnd.onEnter();
        }

        this.periodicSteps.values().forEach(steps -> steps.forEach(BehaviorStep::onEnter));
        this.keyFrames.values().forEach(BehaviorStep::onEnter);
    }

    @Override
    protected void doReset() {
        // Per-run: full reset cascade so any nested cross-entry state (e.g. a LoopBackStep counter
        // used inside a keyframe — unusual but legal) clears between behavior runs.
        if (this.onStart != null) {
            this.onStart.reset();
        }
        if (this.onEnd != null) {
            this.onEnd.reset();
        }

        this.periodicSteps.values().forEach(steps -> steps.forEach(BehaviorStep::reset));
        this.keyFrames.values().forEach(BehaviorStep::reset);
    }

    /**
     * A periodic cadence: fire every {@code interval} ticks, phase-shifted by {@code offset} so the beat
     * can land on a tick other than the cycle boundary (e.g. an animation's strike peak rather than its
     * start). {@code offset} is the tick within each cycle the step fires, constrained to {@code [0, interval)}.
     */
    public record PeriodicSchedule(int interval, int offset) {
        public PeriodicSchedule {
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be greater than 0");
            }
            if (offset < 0 || offset >= interval) {
                throw new IllegalArgumentException("Offset must be within [0, interval)");
            }
        }
    }

    public static <T extends ISettlementsBrainEntity> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends ISettlementsBrainEntity> {

        @Nullable
        private String name;
        @Nullable
        private ITickable tickable;
        @Nonnull
        private final Map<Long, BehaviorStep<T>> keyFrames;
        @Nonnull
        private final Map<PeriodicSchedule, List<BehaviorStep<T>>> periodicSteps;
        @Nullable
        private BehaviorStep<T> onStart;
        @Nullable
        private BehaviorStep<T> onEnd;

        public Builder() {
            this.keyFrames = new HashMap<>();
            this.periodicSteps = new HashMap<>();
        }

        public Builder<T> name(@Nonnull String name) {
            this.name = name;
            return this;
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
            return this.addPeriodicStep(interval, 0, step);
        }

        /**
         * Fire {@code step} every {@code interval} ticks, starting {@code offset} ticks into each cycle, so a
         * beat can align to a mid-cycle moment (e.g. an animation's strike peak) rather than the cycle
         * boundary. {@code offset} must lie within {@code [0, interval)}.
         */
        public Builder<T> addPeriodicStep(int interval, int offset, @Nonnull BehaviorStep<T> step) {
            PeriodicSchedule schedule = new PeriodicSchedule(interval, offset);
            List<BehaviorStep<T>> steps = this.periodicSteps.getOrDefault(schedule, new ArrayList<>());
            steps.add(step);
            this.periodicSteps.put(schedule, steps);
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
            String resolvedName = this.name != null ? this.name
                    : "TimeBasedStep[%s]".formatted(this.tickable.getRemainingCooldownsAsPrettyString());
            TimeBasedStep<T> step = new TimeBasedStep<>(resolvedName, this.tickable, this.onStart, this.onEnd);
            step.addKeyFrames(this.keyFrames);
            step.addPeriodicSteps(this.periodicSteps);
            return step;
        }

    }

}
