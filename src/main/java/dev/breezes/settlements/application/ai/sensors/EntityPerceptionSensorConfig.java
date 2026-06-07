package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "entity_perception", type = ConfigurationType.SENSOR)
public record EntityPerceptionSensorConfig(
        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_range_horizontal",
                description = "Horizontal radius in blocks for shared nearby entity perception scans",
                defaultValue = 32,
                min = 1,
                max = 256)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_range_vertical",
                description = "Vertical radius in blocks for shared nearby entity perception scans",
                defaultValue = 16,
                min = 0,
                max = 64)
        int scanRangeVertical,

        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_interval_seconds",
                description = "How often shared nearby entity perception scans run",
                defaultValue = 10,
                min = 1,
                max = 300)
        int scanIntervalSeconds
) {
}
