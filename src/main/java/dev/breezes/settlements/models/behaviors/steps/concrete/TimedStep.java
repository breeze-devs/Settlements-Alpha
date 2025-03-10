package dev.breezes.settlements.models.behaviors.steps.concrete;

import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.AbstractStep;
import dev.breezes.settlements.models.misc.ITickable;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Represents an action step that automatically transitions to the next step after a certain amount of time
 */
public abstract class TimedStep extends AbstractStep {

    private final ITickable tickable;
    private Status status;

    protected TimedStep(@Nonnull ITickable tickable) {
        super("TimedStep[%s]".formatted(tickable.getRemainingCooldownsAsPrettyString()));
        this.tickable = tickable;
        this.status = Status.STOPPED;
    }

    @Override
    public Optional<Stage> tick(@Nonnull BehaviorContext stateHolder) {
        if (this.status == Status.STOPPED) {
            this.onStart(stateHolder, this.tickable);
            this.status = Status.RUNNING;
            return Optional.empty();
        }

        this.onTick(stateHolder, this.tickable);

        if (this.tickable.tickCheckAndReset(1)) {
            this.onFinish(stateHolder, this.tickable);
            this.status = Status.STOPPED;
        }
        return Optional.empty();
    }

    public abstract void onStart(@Nonnull BehaviorContext stateHolder, @Nonnull ITickable tickable);

    public abstract void onTick(@Nonnull BehaviorContext stateHolder, @Nonnull ITickable tickable);

    public abstract void onFinish(@Nonnull BehaviorContext stateHolder, @Nonnull ITickable tickable);

    private enum Status {
        /**
         * The step is currently running
         */
        RUNNING,

        /**
         * The step is currently stopped
         */
        STOPPED;
    }

}
