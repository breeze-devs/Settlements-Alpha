package dev.breezes.settlements.models.behaviors.steps;

import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.util.crash.CrashUtil;
import dev.breezes.settlements.util.crash.report.BehaviorConfigurationCrashReport;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executes child steps in strict serial order.
 * <p>
 * Step progression contract:
 * <ul>
 *     <li>{@link StepResult.NoOp} - keep executing the current child step</li>
 *     <li>{@link StepResult.Complete} - advance to next child step (without ticking it in the same call)</li>
 *     <li>{@link StepResult.Transition}, {@link StepResult.Fail}, {@link StepResult.Abort} - bubble up</li>
 * </ul>
 * <p>
 * When the final child completes, this step returns {@link StepResult.Complete}.
 */
public class SequencedStep extends AbstractStep {

    private final List<BehaviorStep> steps;
    private int currentStepIndex;

    public SequencedStep(@Nonnull String name, @Nonnull List<BehaviorStep> steps) {
        super("SequencedStep[%s]".formatted(name));

        if (steps.isEmpty()) {
            crashInvalidConfiguration("SequencedStep requires at least one step");
        }

        this.steps = new ArrayList<>(steps);
        this.validate(this.steps);

        this.currentStepIndex = 0;
    }

    @Override
    public StepResult tick(@Nonnull BehaviorContext context) {
        if (this.currentStepIndex >= this.steps.size()) {
            return StepResult.complete();
        }

        BehaviorStep currentStep = this.steps.get(this.currentStepIndex);
        StepResult result = currentStep.tick(context);

        if (result instanceof StepResult.NoOp) {
            return StepResult.noOp();
        }

        if (result instanceof StepResult.Complete) {
            this.currentStepIndex++;
            if (this.currentStepIndex >= this.steps.size()) {
                return StepResult.complete();
            }
            return StepResult.noOp();
        }

        // Transition/Fail/Abort are bubbled up to caller
        return result;
    }

    @Override
    public void reset() {
        this.currentStepIndex = 0;

        Set<BehaviorStep> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BehaviorStep step : this.steps) {
            if (!visited.add(step)) {
                continue;
            }
            step.reset();
        }
    }

    private void validate(@Nonnull List<BehaviorStep> steps) {
        Map<BehaviorStep, Integer> seenByIdentity = new IdentityHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            BehaviorStep step = steps.get(i);
            if (step == null) {
                crashInvalidConfiguration("SequencedStep contains a null child step at index '%s'".formatted(i));
            }

            Integer previousIndex = seenByIdentity.put(step, i);
            if (previousIndex != null) {
                crashInvalidConfiguration(
                        "Each sequence position must use a unique step instance. Duplicate instance used at indices '%s' and '%s'"
                                .formatted(previousIndex, i));
            }
        }
    }

    private static void crashInvalidConfiguration(@Nonnull String message) throws IllegalArgumentException {
        CrashUtil.crash(new BehaviorConfigurationCrashReport(new IllegalArgumentException(message)));
    }

}
