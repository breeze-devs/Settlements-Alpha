package dev.breezes.settlements.application.ai.behavior.usecases.villager.idle;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "walk_dog", type = ConfigurationType.BEHAVIOR)
public record WalkDogConfig(
        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 15,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 30,
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
                identifier = "walk_duration_seconds",
                description = "Maximum duration of a single dog walk, in seconds. Used by both villager and wolf sides.",
                defaultValue = 60,
                min = 10,
                max = 300)
        int walkDurationSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "horizontal_scan_range",
                description = "Horizontal radius (in blocks) within which the villager will look for one of their owned wolves",
                defaultValue = 16,
                min = 5,
                max = 64)
        int horizontalScanRange,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "vertical_scan_range",
                description = "Vertical radius (in blocks) within which the villager will look for one of their owned wolves",
                defaultValue = 4,
                min = 1,
                max = 16)
        int verticalScanRange
) implements BehaviorTimingConfig {

    public WalkDogConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
