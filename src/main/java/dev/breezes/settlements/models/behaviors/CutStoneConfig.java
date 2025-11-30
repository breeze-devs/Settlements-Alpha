package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.BehaviorConfig;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;

@BehaviorConfig(name = "cut_stone", type = ConfigurationType.BEHAVIOR)
public record CutStoneConfig(
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
                defaultValue = 20,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 60,
                min = 1)
        int behaviorCooldownMax
) {

    public CutStoneConfig {
        if (preconditionCheckCooldownMin > preconditionCheckCooldownMax) {
            throw new IllegalArgumentException("preconditionCheckCooldownMin cannot be greater than preconditionCheckCooldownMax");
        }
        if (behaviorCooldownMin > behaviorCooldownMax) {
            throw new IllegalArgumentException("behaviorCooldownMin cannot be greater than behaviorCooldownMax");
        }
    }

}
