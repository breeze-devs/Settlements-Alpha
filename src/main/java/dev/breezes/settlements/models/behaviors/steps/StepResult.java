package dev.breezes.settlements.models.behaviors.steps;

import java.util.Map;

/**
 * Represents the result of a behavior step tick.
 * Replaces the ambiguous Optional<Stage> return type.
 */
public sealed interface StepResult {

    record NoOp() implements StepResult {
    }

    record Complete() implements StepResult {
    }

    record Transition(StageKey key) implements StepResult {
    }

    record Fail(String code, Map<String, Object> details) implements StepResult {
    }

    record Abort(String code, Throwable cause) implements StepResult {
    }

    // Singleton instances for common results to avoid allocation
    StepResult NO_OP = new NoOp();
    StepResult COMPLETE = new Complete();

    static StepResult noOp() {
        return NO_OP;
    }

    static StepResult complete() {
        return COMPLETE;
    }

    static StepResult transition(StageKey key) {
        return new Transition(key);
    }

    static StepResult fail(String code) {
        return new Fail(code, Map.of());
    }

    static StepResult fail(String code, Map<String, Object> details) {
        return new Fail(code, details);
    }

    static StepResult abort(String code, Throwable cause) {
        return new Abort(code, cause);
    }

}
