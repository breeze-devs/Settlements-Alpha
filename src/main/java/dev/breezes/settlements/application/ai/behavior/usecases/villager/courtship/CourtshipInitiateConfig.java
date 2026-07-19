package dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "courtship_initiate", type = ConfigurationType.BEHAVIOR)
public record CourtshipInitiateConfig(
        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 10,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 15,
                min = 1)
        int preconditionCheckCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 120,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 240,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "twins_chance_percent",
                description = "Percent chance (0-100) that a successful courtship produces twins.",
                defaultValue = 3,
                min = 0,
                max = 100)
        int twinsChancePercent,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "conception_chance_percent",
                description = "Percent chance (0-100) that a completed courtship produces a child. On failure "
                        + "the pair still had a successful date and both go on breed cooldown.",
                defaultValue = 70,
                min = 0,
                max = 100)
        int conceptionChancePercent,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "breed_cooldown_seconds",
                description = "Seconds both parents are placed on breed cooldown after every completed "
                        + "courtship, whether it produced a child or ended as a date.",
                defaultValue = 300,
                min = 1,
                max = 3600)
        int breedCooldownSeconds
) implements BehaviorTimingConfig {

    public CourtshipInitiateConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
