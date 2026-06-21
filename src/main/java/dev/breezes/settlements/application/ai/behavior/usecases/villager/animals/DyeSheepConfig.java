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

@BehaviorConfig(name = "dye_sheep", type = ConfigurationType.BEHAVIOR)
public record DyeSheepConfig(

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

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_dye_limit",
                description = "Map of villager expertise level to the maximum number of sheep they can dye in one session.",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "novice", value = "1"),
                        @MapEntry(key = "apprentice", value = "1"),
                        @MapEntry(key = "journeyman", value = "2"),
                        @MapEntry(key = "expert", value = "2"),
                        @MapEntry(key = "master", value = "3")
                })
        Map<String, Integer> expertiseDyeLimit,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 1,
                min = 0)
        int experienceReward) implements BehaviorTimingConfig {

    public DyeSheepConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
