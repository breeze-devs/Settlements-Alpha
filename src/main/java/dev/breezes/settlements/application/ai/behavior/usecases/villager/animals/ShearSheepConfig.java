package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;

import java.util.Map;

/**
 * Configuration Record for the ShearSheepBehaviorV2.
 * <p>
 * This Record is processed by RecordConfigProcessor at startup to generate
 * the corresponding TOML config file and populate ConfigFactory.
 * <p>
 * DESIGN: Using Java Records provides immutability, type safety, and a clean
 * separation between configuration definition and behavior logic.
 */
@BehaviorConfig(name = "shear_sheep", type = ConfigurationType.BEHAVIOR)
public record ShearSheepConfig(

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
                defaultValue = 20, min = 1)
        int preconditionCheckCooldownMax,

        @IntegerConfig(type = ConfigurationType.BEHAVIOR,
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
                description = "Horizontal range (in blocks) to scan for nearby sheep to shear.",
                defaultValue = 32,
                min = 5,
                max = 128)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to scan for nearby sheep to shear.",
                defaultValue = 12,
                min = 1,
                max = 16)
        int scanRangeVertical,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_shear_limit",
                description = "Map of villager expertise level to the maximum number of sheep they can shear in one session.",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "novice", value = "2"),
                        @MapEntry(key = "apprentice", value = "3"),
                        @MapEntry(key = "journeyman", value = "5"),
                        @MapEntry(key = "expert", value = "7"),
                        @MapEntry(key = "master", value = "10")
                })
        Map<String, Integer> expertiseShearLimit) implements BehaviorTimingConfig {

    public ShearSheepConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
