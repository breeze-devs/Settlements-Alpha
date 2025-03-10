package dev.breezes.settlements.models.behaviors.steps.concrete;

import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.steps.AbstractStep;
import dev.breezes.settlements.models.misc.Tickable;
import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * A sub-step that waits for a certain amount of time before transitioning to the next stage
 */
public class WaitStep extends AbstractStep {

    private final Tickable waitTime;
    private final Stage nextStage;

    @Builder
    public WaitStep(@Nonnull Tickable waitTime, @Nonnull Stage nextStage) {
        super("WaitStep[%s]".formatted(waitTime.getRemainingCooldownsAsPrettyString()));
        this.waitTime = waitTime;
        this.nextStage = nextStage;
    }

    @Override
    public Optional<Stage> tick(BehaviorContext context) {
        if (waitTime.tickCheckAndReset(1)) {
            return Optional.of(nextStage);
        }
        return Optional.empty();
    }

}
