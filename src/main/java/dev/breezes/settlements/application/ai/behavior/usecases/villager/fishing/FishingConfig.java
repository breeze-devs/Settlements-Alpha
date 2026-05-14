package dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;

import java.util.Map;

@BehaviorConfig(name = "fishing", type = ConfigurationType.BEHAVIOR)
public record FishingConfig(

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
                defaultValue = 10,
                min = 1)
        int behaviorCooldownMin,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
                description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
                defaultValue = 40,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_horizontal",
                description = "Horizontal range (in blocks) to scan for nearby water.",
                defaultValue = 16,
                min = 5,
                max = 64)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "scan_range_vertical",
                description = "Vertical range (in blocks) to scan for nearby water.",
                defaultValue = 4,
                min = 1,
                max = 16)
        int scanRangeVertical,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "min_wait_time_seconds",
                description = "Minimum time (in seconds) to wait for a fish to bite.",
                defaultValue = 5,
                min = 1,
                max = 60)
        int minWaitTimeSeconds,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "max_wait_time_seconds",
                description = "Maximum time (in seconds) to wait for a fish to bite. For reference, vanilla fishing takes around 5–30s.",
                defaultValue = 15,
                min = 10,
                max = 120)
        int maxWaitTimeSeconds,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_wait_time_scale",
                description = "Scale factor for fishing wait time per expertise level. Lower = faster fishing.",
                deserializer = "StringToDouble",
                defaultValue = {
                        @MapEntry(key = "novice", value = "1.0"),
                        @MapEntry(key = "apprentice", value = "0.95"),
                        @MapEntry(key = "journeyman", value = "0.9"),
                        @MapEntry(key = "expert", value = "0.8"),
                        @MapEntry(key = "master", value = "0.7")
                })
        Map<String, Double> expertiseWaitTimeScale,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 1,
                min = 0)
        int experienceReward

) implements BehaviorTimingConfig {

    public FishingConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
        if (minWaitTimeSeconds > maxWaitTimeSeconds) {
            throw new IllegalArgumentException("minWaitTimeSeconds cannot be greater than maxWaitTimeSeconds");
        }
    }

}
