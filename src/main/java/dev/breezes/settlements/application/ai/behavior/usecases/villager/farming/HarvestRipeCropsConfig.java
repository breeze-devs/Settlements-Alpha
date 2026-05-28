package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "harvest_ripe_crops", type = ConfigurationType.BEHAVIOR)
public record HarvestRipeCropsConfig(

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
                defaultValue = 20,
                min = 1)
        int preconditionCheckCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 60,
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
                identifier = "scan_range_horizontal",
                description = "Horizontal range (in blocks) to scan for nearby ripe crops",
                defaultValue = 16,
                min = 1,
                max = 32)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to scan for nearby ripe crops",
                defaultValue = 4,
                min = 0,
                max = 8)
        int scanRangeVertical,

        @DoubleConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "close_enough_distance",
                description = "Distance (in blocks) at which the villager is considered close enough to begin harvesting",
                defaultValue = 2.0,
                min = 0.5,
                max = 5.0)
        double closeEnoughDistance,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "movement_speed",
                description = "Movement speed multiplier while navigating to a crop",
                defaultValue = 0.5f,
                min = 0.1f,
                max = 1.5f)
        float movementSpeed,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "approach_timeout_ticks",
                description = "Maximum ticks allowed for the villager to reach a crop before re-targeting (20 ticks = 1 second)",
                defaultValue = 400,
                min = 20,
                max = 2000)
        int approachTimeoutTicks,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 2,
                min = 0)
        int experienceReward

) implements BehaviorTimingConfig {

    public HarvestRipeCropsConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
