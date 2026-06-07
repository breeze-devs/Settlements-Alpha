package dev.breezes.settlements.application.ai.behavior.usecases.villager.support;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "throw_potions", type = ConfigurationType.BEHAVIOR)
public record ThrowPotionsConfig(
        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 5,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 10,
                min = 1)
        int preconditionCheckCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 30,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 120,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "minimum_player_reputation",
                description = "Minimum reputation level required to throw a potion at a player",
                defaultValue = 20,
                min = -100,
                max = 100)
        int minimumPlayerReputation,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 2,
                min = 0)
        int experienceReward
) implements BehaviorTimingConfig {

    public ThrowPotionsConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
