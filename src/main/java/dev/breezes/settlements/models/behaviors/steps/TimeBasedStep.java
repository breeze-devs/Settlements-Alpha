package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.SimpleStage;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Executes a series of behavior steps based on the elapsed ticks of a {@link ITickable}
 * <p>
 * If a keyframe is exactly the start or end of the tickable, it will not be executed. Use the onStart and onEnd steps for that.
 */
@CustomLog
public class TimeBasedStep extends AbstractStep {

    private final ITickable tickable;
    /**
     * Map of ticks elapsed to the behavior step to execute
     */
    private final Map<Long, BehaviorStep> keyFrames;

    @Nullable
    private final BehaviorStep onStart;
    @Nullable
    private final BehaviorStep onEnd;

    /**
     * Step to execute on every tick
     */
    @Nullable
    private final BehaviorStep everyTick;

    public TimeBasedStep(@Nonnull ITickable tickable, @Nullable BehaviorStep onStart, @Nullable BehaviorStep onEnd, @Nullable BehaviorStep everyTick) {
        super("TimeBasedStep[%s]".formatted(tickable.getRemainingCooldownsAsPrettyString()));
        this.tickable = tickable;
        this.keyFrames = new HashMap<>();

        this.onStart = onStart;
        this.onEnd = onEnd;
        this.everyTick = everyTick;
    }

    public void addKeyFrames(@Nonnull Map<Long, BehaviorStep> keyFrames) {
        this.keyFrames.putAll(keyFrames);
    }

    @Override
    public Optional<Stage> tick(@Nonnull BehaviorContext context) {
        if (this.onStart != null && this.tickable.getTicksElapsed() == 0) {
            this.onStart.tick(context);
        }

        boolean completed = this.tickable.tickCheckAndReset(1);
        if (this.everyTick != null) {
            this.everyTick.tick(context);
        }

        if (completed) {
            // By default, we return the STEP_END stage
            if (this.onEnd == null) {
                log.behaviorStatus("No onEnd step, returning STEP_END");
                return Optional.of(ControlStages.STEP_END);
            }

            // This return can be overridden by the onEnd step
            Optional<Stage> nextStage = this.onEnd.tick(context);
            log.behaviorStatus("Next stage: %s".formatted(nextStage));
            return nextStage;
        }

        long elapsed = this.tickable.getTicksElapsed();
        if (!this.keyFrames.containsKey(elapsed)) {
            return Optional.empty();
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
        @Nullable
        private BehaviorStep onStart;
        @Nullable
        private BehaviorStep onEnd;
        @Nullable
        private BehaviorStep everyTick;

        public Builder() {
            this.keyFrames = new HashMap<>();
        }

        public Builder withTickable(@Nonnull ITickable tickable) {
            this.tickable = tickable;
            return this;
        }

        public Builder addKeyFrame(@Nonnull Ticks elapsed, @Nonnull BehaviorStep step) {
            this.keyFrames.put(elapsed.getTicks(), step);
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

        public Builder everyTick(@Nonnull BehaviorStep step) {
            this.everyTick = step;
            return this;
        }

        public TimeBasedStep build() {
            if (this.tickable == null) {
                throw new IllegalStateException("Tickable must be set");
            }
            TimeBasedStep step = new TimeBasedStep(this.tickable, this.onStart, this.onEnd, this.everyTick);
            step.addKeyFrames(this.keyFrames);
            return step;
        }

    }

    public static void main(String[] args) {
        Stage timeStage = new SimpleStage("TimeStage");
        TimeBasedStep.Builder timeStepBuilder = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(5).asTickable())
                .onStart(context -> {
                    log.behaviorStatus("Starting timed step");
                    return Optional.empty();
                })
                .onEnd(context -> {
                    log.behaviorStatus("Ending timed step");
                    return Optional.empty();
                });

        // Note: keyframe #0 and #5 will not be executed
        for (int i = 0; i <= 5; i++) {
            int finalI = i;
            timeStepBuilder.addKeyFrame(Ticks.seconds(finalI), context -> {
                log.behaviorStatus("Executing key frame %d".formatted(finalI));
                return Optional.empty();
            });
        }

        StagedStep controlStep = StagedStep.builder()
                .name("ControlStep")
                .initialStage(timeStage)
                .stageStepMap(Map.of(timeStage, timeStepBuilder.build()))
                .nextStage(ControlStages.STEP_END)
                .build();

        BehaviorContext context = new BehaviorContext(null);
        while (controlStep.getCurrentStage() != ControlStages.STEP_END) {
            controlStep.tick(context);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
