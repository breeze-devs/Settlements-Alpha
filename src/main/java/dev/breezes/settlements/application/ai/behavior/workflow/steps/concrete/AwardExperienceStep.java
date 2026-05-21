package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.InteractionOutcomeState;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Awards experience to the initiator villager if {@link InteractionOutcomeState#isSuccess()} is true,
 * then transitions to {@code nextStage}.
 * <p>
 * The XP gating does not gate the transition — successful or not, the step always advances. Even when
 * {@code nextStage} is the behavior's terminal {@code END}, the transition is explicit on purpose:
 * relying on {@link StepResult#complete()} couples the step to the surrounding {@code StagedStep}'s
 * {@code nextStage}, which is fragile when stages get reordered or when AWARD is no longer last.
 */
public class AwardExperienceStep extends AbstractStep<BaseVillager> {

    private final int experienceAmount;
    private final StageKey nextStage;

    @Builder
    private AwardExperienceStep(@Nonnull String name,
                                int experienceAmount,
                                @Nonnull StageKey nextStage,
                                int timeoutTicks,
                                @Nullable StageKey timeoutTransition) {
        super(name, timeoutTicks, timeoutTransition);
        this.experienceAmount = experienceAmount;
        this.nextStage = nextStage;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
        if (GeneralConfig.disableNaturalExperienceGain || this.experienceAmount <= 0) {
            return StepResult.transition(this.nextStage);
        }

        context.getState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.class)
                .filter(InteractionOutcomeState::isSuccess)
                .ifPresent(state -> context.getInitiator().gainExperience(this.experienceAmount));
        return StepResult.transition(this.nextStage);
    }

}
