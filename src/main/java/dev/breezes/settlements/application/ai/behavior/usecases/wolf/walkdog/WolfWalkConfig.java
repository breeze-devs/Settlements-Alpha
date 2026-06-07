package dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.application.config.validation.ConfigRangeValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "wolf_walk", type = ConfigurationType.BEHAVIOR)
public record WolfWalkConfig(
        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
                defaultValue = 1,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 3,
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
                identifier = "walk_duration_seconds",
                description = "Maximum duration of a single wolf-led walk (in seconds)",
                defaultValue = 60,
                min = 10,
                max = 300)
        int walkDurationSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "entity_target_scan_range",
                description = "Radius (in blocks) for sniffable entity targets during a walk",
                defaultValue = 12,
                min = 3,
                max = 32)
        int entityTargetScanRange,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "block_target_horizontal_range",
                description = "Horizontal radius (in blocks) for random block targets during a walk",
                defaultValue = 12,
                min = 3,
                max = 32)
        int blockTargetHorizontalRange,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "block_target_vertical_range",
                description = "Vertical radius (in blocks) for random block targets during a walk",
                defaultValue = 6,
                min = 1,
                max = 16)
        int blockTargetVerticalRange,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "sniff_duration_min_seconds",
                description = "Minimum time a wolf spends sniffing a target (in seconds)",
                defaultValue = 2,
                min = 1,
                max = 10)
        int sniffDurationMinSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "sniff_duration_max_seconds",
                description = "Maximum time a wolf spends sniffing a target (in seconds)",
                defaultValue = 5,
                min = 1,
                max = 15)
        int sniffDurationMaxSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "dig_duration_min_seconds",
                description = "Minimum time a wolf spends digging at a block target (in seconds)",
                defaultValue = 2,
                min = 1,
                max = 10)
        int digDurationMinSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "dig_duration_max_seconds",
                description = "Maximum time a wolf spends digging at a block target (in seconds)",
                defaultValue = 5,
                min = 1,
                max = 15)
        int digDurationMaxSeconds
) implements BehaviorTimingConfig {

    public WolfWalkConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
        ConfigRangeValidator.validateMinLessThanOrEqualMax("sniffDurationSeconds", sniffDurationMinSeconds,
                sniffDurationMaxSeconds);
        ConfigRangeValidator.validateMinLessThanOrEqualMax("digDurationSeconds", digDurationMinSeconds,
                digDurationMaxSeconds);
    }

}
