package dev.breezes.settlements.domain.ai.behavior.contracts;

/**
 * Optional capability implemented by behaviors that can produce a confirmation outcome.
 * <p>
 * When a behavior completes with {@link #didConfirm()} returning {@code true}, the
 * override lane knows to regenerate the day plan so freshly verified information can
 * influence the next plan rather than being ignored.
 */
public interface ConfirmableOverride {

    /**
     * Returns {@code true} if the behavior completed with a positive confirmation outcome.
     * Must be idempotent and never throw; callers read it after the behavior has already stopped.
     */
    boolean didConfirm();

}
