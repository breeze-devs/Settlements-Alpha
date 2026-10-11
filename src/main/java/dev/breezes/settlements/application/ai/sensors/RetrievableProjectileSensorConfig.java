package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "retrievable_projectile", type = ConfigurationType.SENSOR)
public record RetrievableProjectileSensorConfig(
        @IntegerConfig(
                type = ConfigurationType.SENSOR,
                identifier = "scan_interval_seconds",
                description = "How often fletchers scan for nearby stuck arrows and bolts it could retrieve (in seconds).",
                defaultValue = 30,
                min = 1,
                max = 300)
        int scanIntervalSeconds
) {
}
