package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "block_resources", type = ConfigurationType.SENSOR)
public record BlockResourceSensorConfig(
        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_range_horizontal",
                description = "Horizontal radius in blocks for shared block resource scans",
                defaultValue = 32,
                min = 1,
                max = 1024)
        int scanRangeHorizontal,

        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_range_vertical",
                description = "Vertical radius in blocks for shared block resource scans",
                defaultValue = 8,
                min = 0,
                max = 128)
        int scanRangeVertical,

        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_interval_seconds",
                description = "How often shared block resource scans run",
                defaultValue = 30,
                min = 5,
                max = 900)
        int scanIntervalSeconds,

        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_budget_sections_per_tick",
                description = "Maximum block-resource sections scanned per level tick",
                defaultValue = 8,
                min = 1,
                max = 128)
        int scanBudgetSectionsPerTick
) {
}
