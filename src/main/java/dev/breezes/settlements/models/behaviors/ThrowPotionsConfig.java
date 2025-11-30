package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.BehaviorConfig;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;

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
                identifier = "scan_range_horizontal",
                description = "Horizontal range (in blocks) to search for entities that need a potion",
                defaultValue = 16,
                min = 5,
                max = 128)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to search for entities that need a potion",
                defaultValue = 8,
                min = 1,
                max = 16)
        int scanRangeVertical
) {

    public ThrowPotionsConfig {
        if (preconditionCheckCooldownMin > preconditionCheckCooldownMax) {
            throw new IllegalArgumentException("preconditionCheckCooldownMin cannot be greater than preconditionCheckCooldownMax");
        }
        if (behaviorCooldownMin > behaviorCooldownMax) {
            throw new IllegalArgumentException("behaviorCooldownMin cannot be greater than behaviorCooldownMax");
        }
    }

}
