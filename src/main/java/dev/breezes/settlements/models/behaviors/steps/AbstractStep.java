package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import lombok.Getter;

import javax.annotation.Nonnull;
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
     * Executes step logic for one tick and returning the result
     */
    public abstract StepResult tick(@Nonnull BehaviorContext context);

}
