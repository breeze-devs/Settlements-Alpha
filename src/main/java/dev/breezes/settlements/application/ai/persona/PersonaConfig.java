package dev.breezes.settlements.application.ai.persona;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * Configuration record for the async LLM persona-generation pipeline.
 * <p>
 * Generation is non-interactive (no player is waiting on a bubble) so the batch deadline is far
 * more generous than the dialogue sweep's — a slow SIS round trip simply delays when a villager's
 * characterSketch becomes {@code READY}, it never blocks gameplay.
 */
@BehaviorConfig(name = "persona", type = ConfigurationType.GENERAL)
public record PersonaConfig(

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "sweep_interval_ticks",
                description = "How often (in ticks) the pipeline scans loaded villagers for a PENDING persona and dispatches a batch.",
                defaultValue = 1200,
                min = 20,
                max = 12000)
        int sweepIntervalTicks,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "batch_deadline_seconds",
                description = "Wall-clock budget in seconds for one persona generation batch. Generous by design -- generation is offline and non-interactive.",
                defaultValue = 120,
                min = 5,
                max = 600)
        int batchDeadlineSeconds,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "max_batch_size",
                description = "Maximum number of villagers assembled into a single persona generation batch.",
                defaultValue = 32,
                min = 1,
                max = 256)
        int maxBatchSize,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "backoff_floor_ticks",
                description = "Cooldown (in ticks) applied after the first batch that returns zero results, before the pipeline will dispatch another.",
                defaultValue = 200,
                min = 20,
                max = 24000)
        int backoffFloorTicks,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "backoff_ceiling_ticks",
                description = "Upper bound (in ticks) the exponential backoff cooldown may reach after repeated zero-result batches.",
                defaultValue = 12000,
                min = 20,
                max = 72000)
        int backoffCeilingTicks

) {
}
