package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "survey_landscape", type = ConfigurationType.BEHAVIOR)
public record SurveyLandscapeConfig(

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 20,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 40,
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
                defaultValue = 60,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "min_point_distance",
                description = "Minimum horizontal distance (in blocks) from the villager's start position to a valid survey point.",
                defaultValue = 16,
                min = 10,
                max = 256)
        int minPointDistance,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "max_point_distance",
                description = "Maximum horizontal distance (in blocks) from the villager's start position to a valid survey point.",
                defaultValue = 40,
                min = 20,
                max = 512)
        int maxPointDistance,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 2,
                min = 0)
        int experienceReward

) implements BehaviorTimingConfig {

    public SurveyLandscapeConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
        if (minPointDistance >= maxPointDistance) {
            throw new IllegalArgumentException("minPointDistance must be less than maxPointDistance");
        }
    }

}
