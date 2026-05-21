package dev.breezes.settlements.application.ai.behavior.workflow.steps;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.BehaviorConfigurationCrashReport;

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
public class SequencedStep<T extends ISettlementsBrainEntity> extends AbstractStep<T> {

    private final List<BehaviorStep<T>> steps;
    private int currentStepIndex;

    public SequencedStep(@Nonnull String name, @Nonnull List<BehaviorStep<T>> steps) {
        super("SequencedStep[%s]".formatted(name));

        if (steps.isEmpty()) {
            crashInvalidConfiguration("SequencedStep requires at least one step");
        }

        this.steps = new ArrayList<>(steps);
        this.validate(this.steps);

        this.currentStepIndex = 0;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<T> context) {
        if (this.currentStepIndex >= this.steps.size()) {
            return StepResult.complete();
        }

        BehaviorStep<T> currentStep = this.steps.get(this.currentStepIndex);
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
    protected void doOnEnter() {
        this.currentStepIndex = 0;
        this.cascade(BehaviorStep::onEnter);
    }

    @Override
    protected void doReset() {
        this.cascade(BehaviorStep::reset);
    }

    private void cascade(@Nonnull java.util.function.Consumer<BehaviorStep<T>> hook) {
        Set<BehaviorStep<T>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BehaviorStep<T> step : this.steps) {
            if (!visited.add(step)) {
                continue;
            }
            hook.accept(step);
        }
    }

    private void validate(@Nonnull List<BehaviorStep<T>> steps) {
        Map<BehaviorStep<T>, Integer> seenByIdentity = new IdentityHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            BehaviorStep<T> step = steps.get(i);
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
