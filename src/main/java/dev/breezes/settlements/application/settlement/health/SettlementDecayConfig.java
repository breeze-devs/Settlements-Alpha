package dev.breezes.settlements.application.settlement.health;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * How an unhealthy settlement decays.
 */
@BehaviorConfig(name = "settlement_decay", type = ConfigurationType.FEATURE)
public record SettlementDecayConfig(
        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "fading_days",
                description = "Loaded in-game days an unhealthy settlement lasts before it disbands. " +
                        "A settlement is unhealthy without a bell in its claim or with too few living members. " +
                        "0 disables decay.",
                defaultValue = 5,
                min = 0,
                max = 100)
        int fadingDays
) {

}
