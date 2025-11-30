package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.BehaviorConfig;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapEntry;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;

import java.util.Map;

@BehaviorConfig(name = "tame_wolf", type = ConfigurationType.BEHAVIOR)
public record TameWolfConfig(
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
                description = "Horizontal range (in blocks) to scan for nearby wolves to tame",
                defaultValue = 32,
                min = 5,
                max = 128)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to scan for nearby wolves to tame",
                defaultValue = 12,
                min = 1,
                max = 16)
        int scanRangeVertical,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_wolf_limit",
                description = "Map of expertise to the maximum number of wolves a villager can own",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "novice", value = "1"),
                        @MapEntry(key = "apprentice", value = "1"),
                        @MapEntry(key = "journeyman", value = "2"),
                        @MapEntry(key = "expert", value = "2"),
                        @MapEntry(key = "master", value = "3")
                })
        Map<String, Integer> expertiseWolfLimit
) {

    public TameWolfConfig {
        if (preconditionCheckCooldownMin > preconditionCheckCooldownMax) {
            throw new IllegalArgumentException("preconditionCheckCooldownMin cannot be greater than preconditionCheckCooldownMax");
        }
        if (behaviorCooldownMin > behaviorCooldownMax) {
            throw new IllegalArgumentException("behaviorCooldownMin cannot be greater than behaviorCooldownMax");
        }
    }

}
