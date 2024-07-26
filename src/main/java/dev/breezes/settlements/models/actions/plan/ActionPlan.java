package dev.breezes.settlements.models.actions.plan;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
public class ActionPlan<T> implements IActionPlan<T> {

    private final IActionStep<T> startingStep;

    private IActionStep<T> currentStep;
    private boolean complete;

    public ActionPlan(@Nonnull IActionStep<T> startingStep) {
        this.startingStep = startingStep;
        this.currentStep = startingStep;
        this.complete = false;
    }

    @Override
    public void tick(int delta) {
        // Tick the current action step
        this.currentStep.tick(delta);
        if (!this.currentStep.isComplete()) {
            return;
        }

        // Check & progress to the next action step
        Optional<IActionStep<T>> nextStep = this.currentStep.getNextAction();
        if (nextStep.isEmpty()) {
            this.complete = true;
            return;
        }

        this.currentStep = nextStep.get();
    }

}
