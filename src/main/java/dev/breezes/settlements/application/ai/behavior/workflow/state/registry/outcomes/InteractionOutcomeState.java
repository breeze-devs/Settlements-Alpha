package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import lombok.Getter;

/**
 * Per-behavior-run outcome of an interaction (block harvest, entity action, etc.).
 * <p>
 * Producer step (typically the impact keyframe of a bespoke action) calls {@link #markSuccess()} when the
 * world side-effect succeeded. Consumer step ({@code AwardExperienceStep}) reads {@link #isSuccess()} to
 * gate experience reward.
 */
public class InteractionOutcomeState implements BehaviorState {

    @Getter
    private boolean success;

    private InteractionOutcomeState() {
        this.success = false;
    }

    public static InteractionOutcomeState empty() {
        return new InteractionOutcomeState();
    }

    public void markSuccess() {
        this.success = true;
    }

    @Override
    public void reset() {
        this.success = false;
    }

}
