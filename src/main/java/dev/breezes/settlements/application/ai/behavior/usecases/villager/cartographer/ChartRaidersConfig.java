package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;
import org.apache.commons.lang3.Validate;

import java.util.Map;

@BehaviorConfig(name = "chart_raiders", type = ConfigurationType.BEHAVIOR)
public record ChartRaidersConfig(

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "cadence_seconds",
                description = "Seconds a cartographer waits after charting raiders before it charts again",
                defaultValue = 30,
                min = 1,
                max = 600)
        int cadenceSeconds,

        @MapConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "expertise_raiders_per_pulse",
                description = "Most raiders a cartographer charts at once, by expertise level. Each must be at least 1",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "novice", value = "2"),
                        @MapEntry(key = "apprentice", value = "3"),
                        @MapEntry(key = "journeyman", value = "4"),
                        @MapEntry(key = "expert", value = "5"),
                        @MapEntry(key = "master", value = "6")
                })
        Map<String, Integer> expertiseRaidersPerPulse,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER,
                description = BehaviorConfigConstants.EXPERIENCE_REWARD_DESCRIPTION,
                defaultValue = 1,
                min = 0)
        int experienceReward

) {

    public ChartRaidersConfig {
        // A level that charts nobody would still sketch on every pulse, with nothing to show for it
        Validate.isTrue(expertiseRaidersPerPulse.values().stream().allMatch(count -> count >= 1),
                "expertise_raiders_per_pulse values must each be at least 1: %s", expertiseRaidersPerPulse);
    }

    public ClockTicks cadence() {
        return ClockTicks.seconds(this.cadenceSeconds);
    }

}
