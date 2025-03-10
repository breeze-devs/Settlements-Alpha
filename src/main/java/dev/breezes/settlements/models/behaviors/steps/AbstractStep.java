package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the contract for a step in a behavior
 */
public abstract class AbstractStep implements BehaviorStep {

    @Getter
    private final String name;
    @Getter
    private final UUID uuid;

    public AbstractStep(@Nonnull String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    /**
     * Executes step logic for one tick and returning the next stage if the step is complete
     *
     * @return The {@link Stage} to transition to, or empty if the step is not complete
     * <p>
     * A step can return the {@link ControlStages#STEP_END} stage to signal that the step is complete
     */
    public abstract Optional<Stage> tick(@Nonnull BehaviorContext context);

}
