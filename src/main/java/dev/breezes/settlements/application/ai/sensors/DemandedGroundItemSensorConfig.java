package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "demanded_ground_item", type = ConfigurationType.SENSOR)
public record DemandedGroundItemSensorConfig(
        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_interval_seconds",
                description = "How often each villager scans for nearby demanded ground items (in seconds)",
                defaultValue = 10,
                min = 1,
                max = 300)
        int scanIntervalSeconds
) {
}
