package dev.breezes.settlements.application.hunger;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "hunger", type = ConfigurationType.BEHAVIOR)
public record HungerConfig(
        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "tick_interval_seconds",
                description = "How often hunger drains in seconds.",
                defaultValue = 10,
                min = 1,
                max = 3600)
        int tickIntervalSeconds,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "drain_per_interval",
                description = "Baseline hunger drained each hunger interval.",
                defaultValue = 0.003f,
                min = 0.0f,
                max = 1.0f)
        float drainPerInterval,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "sleeping_drain_multiplier",
                description = "Multiplier applied while the villager is sleeping.",
                defaultValue = 0.25f,
                min = 0.0f,
                max = 10.0f)
        float sleepingDrainMultiplier,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "hunger_bonus_threshold",
                description = "Hunger threshold at or above which villagers receive a cooldown bonus.",
                defaultValue = 0.75f,
                min = 0.0f,
                max = 1.0f)
        float hungerBonusThreshold,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "cooldown_bonus_multiplier",
                description = "Cooldown multiplier applied when the villager is well-fed.",
                defaultValue = 0.8f,
                min = 0.0f,
                max = 10.0f)
        float cooldownBonusMultiplier,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "cooldown_scale_start_threshold",
                description = "Hunger threshold where cooldown penalties begin scaling up.",
                defaultValue = 0.5f,
                min = 0.0f,
                max = 1.0f)
        float cooldownScaleStartThreshold,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "max_cooldown_multiplier",
                description = "Maximum cooldown multiplier applied at zero hunger.",
                defaultValue = 3.0f,
                min = 1.0f,
                max = 10.0f)
        float maxCooldownMultiplier,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "effect_start_threshold",
                description = "Hunger threshold where weakness and slowness begin to apply.",
                defaultValue = 0.3f,
                min = 0.0f,
                max = 1.0f)
        float effectStartThreshold,

        @FloatConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "eat_priority_threshold",
                description = "Villagers will try to eat when hunger falls below this threshold.",
                defaultValue = 0.7f,
                min = 0.0f,
                max = 1.0f)
        float eatPriorityThreshold
) {

}
