package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

import dev.breezes.settlements.application.ai.behavior.runtime.timing.BehaviorTimingConfig;
import dev.breezes.settlements.application.config.constants.BehaviorConfigConstants;
import dev.breezes.settlements.application.config.validation.BehaviorCooldownValidator;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "donate_emeralds", type = ConfigurationType.BEHAVIOR)
public record DonationConfig(
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
                defaultValue = 120,
                min = 1)
        int behaviorCooldownMax,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "floor",
                description = "Emerald balance below which a villager is considered destitute and eligible to receive a donation",
                defaultValue = 50,
                min = 1)
        int floor,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "min_donation",
                description = "Minimum number of emeralds worth giving in a single donation",
                defaultValue = 4,
                min = 1)
        int minDonation,

        @IntegerConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "base_comfort",
                description = "Baseline emerald cushion a donor keeps before donating, before genetics adjustment",
                defaultValue = 64,
                min = 0)
        int baseComfort,

        @DoubleConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "comfort_impact",
                description = "How strongly CHARISMA lowers the donor's comfort cushion (0-1); higher CHARISMA gives sooner",
                defaultValue = 0.4,
                min = 0.0,
                max = 1.0)
        double comfortImpact,

        @DoubleConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "base_chance",
                description = "Baseline probability (0-1) that an eligible donor actually donates on a given check",
                defaultValue = 0.5,
                min = 0.0,
                max = 1.0)
        double baseChance,

        @DoubleConfig(
                type = ConfigurationType.BEHAVIOR,
                identifier = "chance_impact",
                description = "How strongly CHARISMA raises the donation chance (0-1); higher CHARISMA donates more often",
                defaultValue = 0.6,
                min = 0.0,
                max = 1.0)
        double chanceImpact
) implements BehaviorTimingConfig {

    public DonationConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }

}
