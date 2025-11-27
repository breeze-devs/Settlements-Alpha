package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.BehaviorConfig;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapEntry;

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
                identifier = "precondition_check_cooldown_min",
                description = "Minimum cooldown (in ticks) between precondition checks. Lower values make villagers check more frequently for sheep to shear.",
                defaultValue = 10,
                min = 1)
        int preconditionCheckCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "precondition_check_cooldown_max",
                description = "Maximum cooldown (in ticks) between precondition checks. Higher values make villagers check less frequently.",
                defaultValue = 20, min = 1)
        int preconditionCheckCooldownMax,

        @IntegerConfig(type = ConfigurationType.BEHAVIOR,
                identifier = "behavior_cooldown_min",
                description = "Minimum cooldown (in ticks) before the behavior can run again after completion.",
                defaultValue = 60,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "behavior_cooldown_max",
                description = "Maximum cooldown (in ticks) before the behavior can run again after completion.",
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
        Map<String, Integer> expertiseShearLimit) {

    /**
     * Validation constructor (optional but recommended).
     * Records can have a compact constructor for validation.
     */
    public ShearSheepConfig {
        if (preconditionCheckCooldownMin > preconditionCheckCooldownMax) {
            throw new IllegalArgumentException("preconditionCheckCooldownMin cannot be greater than preconditionCheckCooldownMax");
        }
        if (behaviorCooldownMin > behaviorCooldownMax) {
            throw new IllegalArgumentException("behaviorCooldownMin cannot be greater than behaviorCooldownMax");
        }
    }

}
