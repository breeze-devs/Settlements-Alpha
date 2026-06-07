package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;

import java.util.Map;

@BehaviorConfig(name = "butcher_livestock", type = ConfigurationType.BEHAVIOR)
public record ButcherLivestockConfig(
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
                defaultValue = 20,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 30,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_horizontal",
                description = "Horizontal range (in blocks) to scan for nearby livestock to butcher.",
                defaultValue = 32,
                min = 5,
                max = 128)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to scan for nearby livestock to butcher.",
                defaultValue = 12,
                min = 1,
                max = 16)
        int scanRangeVertical,

        @BooleanConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "require_village_owned_tag",
                description = "Whether only village-owned animals can be butchered.",
                defaultValue = false)
        boolean requireVillageOwnedTag,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "minimum_keep_count",
                description = "Map of animal entity id to the minimum nearby count to keep before butchering surplus. Also acts as the allow-list of butcherable animal types.",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "minecraft:cow", value = "4"),
                        @MapEntry(key = "minecraft:sheep", value = "6"),
                        @MapEntry(key = "minecraft:chicken", value = "4"),
                        @MapEntry(key = "minecraft:pig", value = "3"),
                        @MapEntry(key = "minecraft:rabbit", value = "4")
                })
        Map<String, Integer> minimumKeepCount,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_butcher_limit",
                description = "Map of villager expertise level to the maximum number of animals they can butcher in one session.",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "novice", value = "3"),
                        @MapEntry(key = "apprentice", value = "5"),
                        @MapEntry(key = "journeyman", value = "8"),
                        @MapEntry(key = "expert", value = "12"),
                        @MapEntry(key = "master", value = "20")
                })
        Map<String, Integer> expertiseButcherLimit,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 1,
                min = 0)
        int experienceReward
) implements BehaviorTimingConfig {

    public ButcherLivestockConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
